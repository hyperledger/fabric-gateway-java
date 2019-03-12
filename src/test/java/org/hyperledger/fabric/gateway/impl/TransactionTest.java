/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hyperledger.fabric.gateway.GatewayException;
import org.hyperledger.fabric.gateway.Transaction;
import org.hyperledger.fabric.sdk.ChaincodeResponse;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class TransactionTest {
  TransactionProposalRequest request = null;
  Channel channel = null;
  Transaction transaction = null;

  @Before
  public void setup() throws Exception {
    request = mock(TransactionProposalRequest.class);
    channel = mock(Channel.class);
    transaction = new TransactionImpl(request, channel);
  }

  @Test
  public void testGetName() {
    when(request.getFcn()).thenReturn("txn");
    String name = transaction.getName();
    Assert.assertEquals(name, "txn");
  }

  @Test
  public void testGetTransactionId() {
    String txid = transaction.getTransactionId();
    Assert.assertNull(txid);
  }

  @Test
  public void testSetTransient() {
    transaction.setTransient(null);
    // TODO
  }

  @Test
  public void testEvaluateNoResponses() throws Exception {
    try {
      List<ProposalResponse> responses = new ArrayList<>();
      when(channel.sendTransactionProposal(request)).thenReturn(responses);

      transaction.evaluate("arg1");
      Assert.fail("Should have thrown GatewayException");
    } catch (GatewayException e) {
      Assert.assertEquals(e.getMessage(), "No valid proposal responses received.");
    }
  }

  @Test
  public void testEvaluateUnsuccessfulResponse() throws Exception {
    try {
      List<ProposalResponse> responses = new ArrayList<>();
      ProposalResponse response = mock(ProposalResponse.class);
      when(response.getStatus()).thenReturn(ChaincodeResponse.Status.FAILURE);
      responses.add(response);
      when(channel.sendTransactionProposal(request)).thenReturn(responses);

      transaction.evaluate("arg1");
      Assert.fail("should have thrown GatewayException");
    } catch (GatewayException e) {
      Assert.assertEquals(e.getMessage(), "No valid proposal responses received.");
    }
  }

  @Test
  public void testEvaluateSuccess() throws Exception {
    List<ProposalResponse> responses = new ArrayList<>();
    ProposalResponse response = mock(ProposalResponse.class);
    when(response.getStatus()).thenReturn(ChaincodeResponse.Status.SUCCESS);
    when(response.getChaincodeActionResponsePayload()).thenReturn("successful result".getBytes());
    responses.add(response);
    when(channel.sendTransactionProposal(request)).thenReturn(responses);
    when(channel.sendTransaction(responses)).thenReturn(CompletableFuture.completedFuture(null));

    byte[] result = transaction.evaluate("arg1");
    Assert.assertEquals(new String(result), "successful result");
  }

  @Test
  public void testSubmitNoResponses() throws Exception {
    try {
      List<ProposalResponse> responses = new ArrayList<>();
      when(channel.sendTransactionProposal(request)).thenReturn(responses);

      transaction.submit("arg1");
      Assert.fail("should have thrown GatewayException");
    } catch (GatewayException e) {
      Assert.assertEquals(e.getMessage(), "No valid proposal responses received.");
    }
  }

  @Test
  public void testSubmitUnsuccessfulResponse() throws Exception {
    try {
      List<ProposalResponse> responses = new ArrayList<>();
      ProposalResponse response = mock(ProposalResponse.class);
      when(response.getStatus()).thenReturn(ChaincodeResponse.Status.FAILURE);
      responses.add(response);
      when(channel.sendTransactionProposal(request)).thenReturn(responses);

      transaction.submit("arg1");
      Assert.fail("should have thrown GatewayException");
    } catch (GatewayException e) {
      Assert.assertEquals(e.getMessage(), "No valid proposal responses received.");
    }
  }

  @Test
  public void testSubmitSuccess() throws Exception {
    List<ProposalResponse> responses = new ArrayList<>();
    ProposalResponse response = mock(ProposalResponse.class);
    when(response.getStatus()).thenReturn(ChaincodeResponse.Status.SUCCESS);
    when(response.getChaincodeActionResponsePayload()).thenReturn("successful result".getBytes());
    responses.add(response);
    when(channel.sendTransactionProposal(request)).thenReturn(responses);
    when(channel.sendTransaction(responses)).thenReturn(CompletableFuture.completedFuture(null));

    byte[] result = transaction.submit("arg1");
    Assert.assertEquals(new String(result), "successful result");
  }

}
