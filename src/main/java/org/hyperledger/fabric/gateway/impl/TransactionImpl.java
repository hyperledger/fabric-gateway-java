/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.gateway.GatewayException;
import org.hyperledger.fabric.gateway.Transaction;
import org.hyperledger.fabric.gateway.spi.CommitHandler;
import org.hyperledger.fabric.gateway.spi.CommitHandlerFactory;
import org.hyperledger.fabric.gateway.spi.Query;
import org.hyperledger.fabric.gateway.spi.QueryHandler;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.ChaincodeResponse;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.QueryByChaincodeRequest;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class TransactionImpl implements Transaction {
    private static final Log logger = LogFactory.getLog(TransactionImpl.class);

    private final ContractImpl contract;
    private final String name;
    private final CommitHandlerFactory commitHandlerFactory;
    private TimePeriod commitTimeout;
    private final QueryHandler queryHandler;

    TransactionImpl(ContractImpl contract, String name) {
        this.contract = contract;
        this.name = name;
        NetworkImpl network = contract.getNetwork();
        GatewayImpl gateway = network.getGateway();
        commitHandlerFactory = gateway.getCommitHandlerFactory();
        commitTimeout = gateway.getCommitTimeout();
        queryHandler = network.getQueryHandler();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setTransient(Map<String, byte[]> transientData) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setCommitTimeout(long timeout, TimeUnit timeUnit) {
        commitTimeout = new TimePeriod(timeout, timeUnit);
    }

    @Override
    public byte[] submit(String... args) throws GatewayException, TimeoutException {
        try {
            NetworkImpl network = contract.getNetwork();
            Channel channel = network.getChannel();

            TransactionProposalRequest request = network.getGateway().getClient().newTransactionProposalRequest();
            request.setChaincodeID(ChaincodeID.newBuilder().setName(contract.getChaincodeId()).build());
            request.setFcn(name);
            request.setArgs(args);

            Collection<ProposalResponse> proposalResponses = channel.sendTransactionProposal(request);

            // Validate the proposal responses.
            Collection<ProposalResponse> validResponses = validatePeerResponses(proposalResponses);

            if (validResponses.size() < 1) {
                logger.error("No valid proposal responses received.");
                throw new GatewayException("No valid proposal responses received.");
            }
            ProposalResponse proposalResponse = validResponses.iterator().next();
            byte[] result = proposalResponse.getChaincodeActionResponsePayload();
            String transactionId = proposalResponse.getTransactionID();

            Channel.TransactionOptions transactionOptions = Channel.TransactionOptions.createTransactionOptions()
                    .nOfEvents(Channel.NOfEvents.createNoEvents()); // Disable default commit wait behaviour

            CommitHandler commitHandler = commitHandlerFactory.create(transactionId, network);
            commitHandler.startListening();

            try {
                channel.sendTransaction(proposalResponses, transactionOptions).get(60, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                commitHandler.cancelListening();
                throw e;
            } catch (Exception e) {
                commitHandler.cancelListening();
                throw new GatewayException("Failed to send transaction to the orderer", e);
            }

            commitHandler.waitForEvents(commitTimeout.getTime(), commitTimeout.getTimeUnit());

            return result;
        } catch (InvalidArgumentException | ProposalException e) {
            throw new GatewayException(e);
        }
    }

    private Collection<ProposalResponse> validatePeerResponses(Collection<ProposalResponse> proposalResponses) {
        final Collection<ProposalResponse> validResponses = new ArrayList<>();
        proposalResponses.forEach(response -> {
            String peerUrl = response.getPeer() != null ? response.getPeer().getUrl() : "<unknown>";
            if (response.getStatus().equals(ChaincodeResponse.Status.SUCCESS)) {
                logger.debug(String.format("validatePeerResponses: valid response from peer %s", peerUrl));
                validResponses.add(response);
            } else {
                logger.warn(String.format("validatePeerResponses: invalid response from peer %s, message %s", peerUrl, response.getMessage()));
            }
        });

        return validResponses;
    }

    @Override
    public byte[] evaluate(String... args) throws GatewayException {
        NetworkImpl network = contract.getNetwork();

        QueryByChaincodeRequest request = network.getGateway().getClient().newQueryProposalRequest();
        ChaincodeID chaincodeId = ChaincodeID.newBuilder().setName(contract.getChaincodeId()).build();
        request.setChaincodeID(chaincodeId);
        request.setFcn(name);
        request.setArgs(args);

        Query query = new QueryImpl(network.getChannel(), request);
        ProposalResponse response = queryHandler.evaluate(query);
        try {
            return response.getChaincodeActionResponsePayload();
        } catch (InvalidArgumentException e) {
            throw new GatewayException(e);
        }
    }

}
