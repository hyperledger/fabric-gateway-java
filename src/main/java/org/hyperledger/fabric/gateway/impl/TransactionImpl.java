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
import org.hyperledger.fabric.sdk.ChaincodeResponse;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.ProposalResponse;
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
    private final TransactionProposalRequest request;
    private final NetworkImpl network;
    private final CommitHandlerFactory commitHandlerFactory;

    TransactionImpl(TransactionProposalRequest request, NetworkImpl network) {
        this.request = request;
        this.network = network;
        this.commitHandlerFactory = network.getGateway().getCommitHandlerFactory();
    }

    @Override
    public String getName() {
        return request.getFcn();
    }

    @Override
    public void setTransient(Map<String, byte[]> transientData) {
        // TODO Auto-generated method stub

    }

    @Override
    public byte[] submit(String... args) throws GatewayException, TimeoutException {
        try {
            Channel channel = network.getChannel();
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

            commitHandler.waitForEvents();

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
        try {
            Channel channel = network.getChannel();
            request.setArgs(args);
            Collection<ProposalResponse> proposalResponses = channel.sendTransactionProposal(request);

            // Validate the proposal responses.
            Collection<ProposalResponse> validResponses = validatePeerResponses(proposalResponses);

            if (validResponses.size() < 1) {
                logger.error("No valid proposal responses received.");
                throw new GatewayException("No valid proposal responses received.");
            }
            ProposalResponse proposalResponse = validResponses.iterator().next();
            return proposalResponse.getChaincodeActionResponsePayload();
        } catch (InvalidArgumentException | ProposalException e) {
            throw new GatewayException(e);
        }
    }

}
