/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.gateway.GatewayException;
import org.hyperledger.fabric.gateway.Transaction;
import org.hyperledger.fabric.sdk.ChaincodeResponse;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;

public final class TransactionImpl implements Transaction {
  private static final Log logger = LogFactory.getLog(TransactionImpl.class);
  private final TransactionProposalRequest request;
  private final Channel channel;
  private String transactionId = null;

  TransactionImpl(TransactionProposalRequest request, Channel channel) {
    this.request = request;
    this.channel = channel;
  }

  @Override
  public String getName() {
    return request.getFcn();
  }

  @Override
  public String getTransactionId() {
    return transactionId;
  }

  @Override
  public void setTransient(Map<String, byte[]> transientData) {
    // TODO Auto-generated method stub

  }

  @Override
  public byte[] submit(String... args)  throws GatewayException, TimeoutException {
    try {
      request.setArgs(args);
      Collection<ProposalResponse> proposalResponses = channel.sendTransactionProposal(request);

      // Validate the proposal responses.
      Collection<ProposalResponse> validResponses = validatePeerResponses(proposalResponses);

      if (validResponses.size() < 1) {
          logger.error("No valid proposal responses received.");
          throw new GatewayException("No valid proposal responses received.");
      }
      ProposalResponse proposalResponse = validResponses.iterator().next();
      transactionId = proposalResponse.getTransactionID();
      byte[] result = proposalResponse.getChaincodeActionResponsePayload();
      channel.sendTransaction(proposalResponses).get(60, TimeUnit.SECONDS);
      return result;
    } catch (InvalidArgumentException | InterruptedException | ExecutionException | ProposalException e) {
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
    request.setArgs(args);
    Collection<ProposalResponse> proposalResponses = channel.sendTransactionProposal(request);

    // Validate the proposal responses.
    Collection<ProposalResponse> validResponses = validatePeerResponses(proposalResponses);

    if (validResponses.size() < 1) {
        logger.error("No valid proposal responses received.");
        throw new GatewayException("No valid proposal responses received.");
    }
    ProposalResponse proposalResponse = validResponses.iterator().next();
    transactionId = proposalResponse.getTransactionID();
    return proposalResponse.getChaincodeActionResponsePayload();
    } catch (InvalidArgumentException | ProposalException e) {
      throw new GatewayException(e);
    }
  }

}
