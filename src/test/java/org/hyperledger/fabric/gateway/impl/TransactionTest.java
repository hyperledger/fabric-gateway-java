/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import org.hamcrest.CoreMatchers;
import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.GatewayException;
import org.hyperledger.fabric.gateway.TestUtils;
import org.hyperledger.fabric.gateway.spi.CommitHandler;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;


public class TransactionTest {
    private final TestUtils testUtils = TestUtils.getInstance();
    private final Timeout timeout = new Timeout(7, TimeUnit.DAYS);
    private TransactionProposalRequest request;
    private Channel channel;
    private Contract contract;
    private CommitHandler commitHandler;

    @BeforeEach
    public void setup() throws Exception {
        channel = mock(Channel.class);
        when(channel.sendTransaction(ArgumentMatchers.anyCollection(), ArgumentMatchers.any(Channel.TransactionOptions.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        HFClient client = mock(HFClient.class);
        when(client.getChannel(ArgumentMatchers.anyString())).thenReturn(channel);
        when(client.newTransactionProposalRequest()).thenReturn(HFClient.createNewInstance().newTransactionProposalRequest());

        request = mock(TransactionProposalRequest.class);

        commitHandler = mock(CommitHandler.class);
        Gateway gateway = TestUtils.getInstance().newGatewayBuilder()
                .client(client)
                .commitHandler((transactionId, network) -> commitHandler)
                .commitTimeout(timeout.getTimeout(), timeout.getTimeUnit())
                .connect();
        contract = gateway.getNetwork("network").getContract("contract");
    }

    @Test
    public void testGetName() {
        String name = "txn";
        String result = contract.createTransaction(name).getName();
        assertThat(result, CoreMatchers.equalTo(name));
    }

    @Test
    public void testSetTransient() {
        contract.createTransaction("txn").setTransient(null);
        // TODO
    }

    @Test
    public void testEvaluateNoResponses() throws Exception {
        when(channel.sendTransactionProposal(request)).thenReturn(Collections.emptyList());

        assertThrows(GatewayException.class, () -> contract.evaluateTransaction("txn", "arg1"));
    }

    @Test
    public void testEvaluateUnsuccessfulResponse() throws Exception {
        ProposalResponse response = testUtils.newFailureProposalResponse(new byte[0]);
        when(channel.sendTransactionProposal(request)).thenReturn(Arrays.asList(response));

        assertThrows(GatewayException.class, () -> contract.evaluateTransaction("txn", "arg1"));
    }

    @Test
    public void testEvaluateSuccess() throws Exception {
        String expected = "successful result";
        ProposalResponse response = testUtils.newSuccessfulProposalResponse(expected.getBytes());
        when(channel.sendTransactionProposal(ArgumentMatchers.any())).thenReturn(Arrays.asList(response));

        byte[] result = contract.evaluateTransaction("txn", "arg1");
        assertThat(new String(result), CoreMatchers.equalTo(expected));
    }

    @Test
    public void testSubmitNoResponses() throws Exception {
        List<ProposalResponse> responses = new ArrayList<>();
        when(channel.sendTransactionProposal(request)).thenReturn(responses);

        assertThrows(GatewayException.class, () -> contract.submitTransaction("txn", "arg1"));
    }

    @Test
    public void testSubmitUnsuccessfulResponse() throws Exception {
        ProposalResponse response = testUtils.newFailureProposalResponse(new byte[0]);
        when(channel.sendTransactionProposal(request)).thenReturn(Arrays.asList(response));

        assertThrows(GatewayException.class, () -> contract.submitTransaction("txn", "arg1"));
    }

    @Test
    public void testSubmitSuccess() throws Exception {
        String expected = "successful result";
        ProposalResponse response = testUtils.newSuccessfulProposalResponse(expected.getBytes());
        when(channel.sendTransactionProposal(ArgumentMatchers.any())).thenReturn(Arrays.asList(response));

        byte[] result = contract.submitTransaction("txn", "arg1");
        assertThat(new String(result), equalTo(expected));
    }

    @Test
    public void testUsesGatewayCommitTimeout() throws Exception {
        String expected = "successful result";
        ProposalResponse response = testUtils.newSuccessfulProposalResponse(expected.getBytes());
        when(channel.sendTransactionProposal(ArgumentMatchers.any())).thenReturn(Arrays.asList(response));

        contract.submitTransaction("txn", "arg1");

        verify(commitHandler).waitForEvents(timeout.getTimeout(), timeout.getTimeUnit());
    }
}
