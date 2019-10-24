/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.gateway.ContractException;
import org.hyperledger.fabric.gateway.GatewayRuntimeException;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.Transaction;
import org.hyperledger.fabric.gateway.TransactionResponse;
import org.hyperledger.fabric.gateway.spi.CommitHandler;
import org.hyperledger.fabric.gateway.spi.CommitHandlerFactory;
import org.hyperledger.fabric.gateway.spi.Query;
import org.hyperledger.fabric.gateway.spi.QueryHandler;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.ChaincodeResponse;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.QueryByChaincodeRequest;
import org.hyperledger.fabric.sdk.ServiceDiscovery;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.hyperledger.fabric.sdk.TransactionRequest;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.ServiceDiscoveryException;

import javax.annotation.Nullable;

import static org.hyperledger.fabric.sdk.Channel.DiscoveryOptions.createDiscoveryOptions;

public final class TransactionImpl implements Transaction {
    private static final Log logger = LogFactory.getLog(TransactionImpl.class);

    private final ContractImpl contract;
    private final String name;
    private final NetworkImpl network;
    private final Channel channel;
    private final GatewayImpl gateway;
    private final CommitHandlerFactory commitHandlerFactory;
    private TimePeriod commitTimeout;
    private final QueryHandler queryHandler;
    private Map<String, byte[]> transientData = null;
    private Collection<Peer> endorsingPeers = null;

    TransactionImpl(ContractImpl contract, String name) {
        this.contract = contract;
        this.name = name;
        network = contract.getNetwork();
        channel = network.getChannel();
        gateway = network.getGateway();
        commitHandlerFactory = gateway.getCommitHandlerFactory();
        commitTimeout = gateway.getCommitTimeout();
        queryHandler = network.getQueryHandler();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Transaction setTransient(Map<String, byte[]> transientData) {
        this.transientData = transientData;
        return this;
    }

    @Override
    public Transaction setCommitTimeout(long timeout, TimeUnit timeUnit) {
        commitTimeout = new TimePeriod(timeout, timeUnit);
        return this;
    }

    @Override
    public Transaction setEndorsingPeers(Collection<Peer> peers) {
        endorsingPeers = peers;
        return this;
    }

    @Override
    public TransactionResponse submit(User userContext, String... args) throws ContractException, TimeoutException, InterruptedException {
        return createSubmit(userContext, args);
    }

    @Override
    public TransactionResponse submit(String... args) throws ContractException, TimeoutException, InterruptedException {
        return createSubmit(null, args);
    }

    private TransactionResponse createSubmit(@Nullable User userContext, String... args) throws ContractException, TimeoutException, InterruptedException {
        try {
            TransactionProposalRequest request = newProposalRequest(userContext, args);
            Collection<ProposalResponse> proposalResponses = sendTransactionProposal(request);

            Collection<ProposalResponse> validResponses = validatePeerResponses(proposalResponses);
            ProposalResponse proposalResponse = validResponses.iterator().next();
            byte[] result = proposalResponse.getChaincodeActionResponsePayload();
            String transactionId = proposalResponse.getTransactionID();

            Channel.TransactionOptions transactionOptions = Channel.TransactionOptions.createTransactionOptions()
                    .nOfEvents(Channel.NOfEvents.createNoEvents()); // Disable default commit wait behaviour

            CommitHandler commitHandler = commitHandlerFactory.create(transactionId, network);
            commitHandler.startListening();

            try {
                channel.sendTransaction(validResponses, transactionOptions).get(60, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                commitHandler.cancelListening();
                throw e;
            } catch (Exception e) {
                commitHandler.cancelListening();
                throw new ContractException("Failed to send transaction to the orderer", e);
            }

            commitHandler.waitForEvents(commitTimeout.getTime(), commitTimeout.getTimeUnit());

            return new TransactionResponse(transactionId, result);
        } catch (InvalidArgumentException | ProposalException | ServiceDiscoveryException e) {
            throw new GatewayRuntimeException(e);
        }
    }
    private TransactionProposalRequest newProposalRequest(@Nullable User userContext, String[] args) {
        TransactionProposalRequest request = network.getGateway().getClient().newTransactionProposalRequest();
        configureRequest(userContext, request, args);
        if (transientData != null) {
            try {
                request.setTransientMap(transientData);
            } catch (InvalidArgumentException e) {
                // Only happens if transientData is null
                throw new IllegalStateException(e);
            }
        }
        return request;
    }

    private void configureRequest(@Nullable User userContext, TransactionRequest request, String[] args) {
        request.setChaincodeID(getChaincodeId());
        request.setFcn(name);
        request.setArgs(args);
        if (userContext != null) {
            request.setUserContext(userContext);
        }
    }

    private ChaincodeID getChaincodeId() {
        return ChaincodeID.newBuilder()
                .setName(contract.getChaincodeId())
                .build();
    }

    private Collection<ProposalResponse> sendTransactionProposal(TransactionProposalRequest request) throws InvalidArgumentException, ServiceDiscoveryException, ProposalException {
        if (endorsingPeers != null) {
            return channel.sendTransactionProposal(request, endorsingPeers);
        } else if (network.getGateway().isDiscoveryEnabled()) {
            Channel.DiscoveryOptions discoveryOptions = createDiscoveryOptions()
                    .setEndorsementSelector(ServiceDiscovery.EndorsementSelector.ENDORSEMENT_SELECTION_RANDOM)
                    .setForceDiscovery(true);
            return channel.sendTransactionProposalToEndorsers(request, discoveryOptions);
        } else {
            return channel.sendTransactionProposal(request);
        }
    }

    private Collection<ProposalResponse> validatePeerResponses(Collection<ProposalResponse> proposalResponses) throws ContractException {
        final Collection<ProposalResponse> validResponses = new ArrayList<>();
        final Collection<String> invalidResponseMsgs = new ArrayList<>();
        proposalResponses.forEach(response -> {
            String peerUrl = response.getPeer() != null ? response.getPeer().getUrl() : "<unknown>";
            if (response.getStatus().equals(ChaincodeResponse.Status.SUCCESS)) {
                logger.debug(String.format("validatePeerResponses: valid response from peer %s", peerUrl));
                validResponses.add(response);
            } else {
                logger.warn(String.format("validatePeerResponses: invalid response from peer %s, message %s", peerUrl, response.getMessage()));
                invalidResponseMsgs.add(response.getMessage());
            }
        });

        if(validResponses.size() < 1) {
        	String msg = String.format("No valid proposal responses received. %d peer error responses: %s",
        			invalidResponseMsgs.size(), String.join("; ", invalidResponseMsgs));
            logger.error(msg);
            throw new ContractException(msg);
        }

        return validResponses;
    }

    @Override
    public TransactionResponse evaluate(User userContext, String... args) throws ContractException {
        return processEvaluate(userContext, args);
    }

    @Override
    public TransactionResponse evaluate(String... args) throws ContractException {
        return processEvaluate(null, args);
    }

    private TransactionResponse processEvaluate(@Nullable User userContext, String... args) throws ContractException {
        QueryByChaincodeRequest request = newQueryRequest(userContext, args);
        Query query = new QueryImpl(network.getChannel(), request);

        ProposalResponse response = queryHandler.evaluate(query);

        try {
            return new TransactionResponse(response.getTransactionID(), response.getChaincodeActionResponsePayload());
        } catch (InvalidArgumentException e) {
            throw new ContractException(response.getMessage(), e);
        }
    }

    private QueryByChaincodeRequest newQueryRequest(@Nullable User userContext, String[] args) {
        QueryByChaincodeRequest request = gateway.getClient().newQueryProposalRequest();
        configureRequest(userContext, request, args);
        if (transientData != null) {
            try {
                request.setTransientMap(transientData);
            } catch (InvalidArgumentException e) {
                // Only happens if transientData is null
                throw new IllegalStateException(e);
            }
        }
        return request;
    }
}
