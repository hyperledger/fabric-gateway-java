/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.ContractException;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.GatewayException;
import org.hyperledger.fabric.gateway.TestUtils;
import org.hyperledger.fabric.gateway.spi.CommitHandler;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyCollection;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TransactionTest {
    private final TestUtils testUtils = TestUtils.getInstance();
    private final TimePeriod timeout = new TimePeriod(7, TimeUnit.DAYS);
    private Gateway gateway;
    private Channel channel;
    private Contract contract;
    private CommitHandler commitHandler;
    private Peer peer;
    private ProposalResponse failureResponse;
    private Map<String, byte[]> transientMap;

    @Captor
    private ArgumentCaptor<Collection<ProposalResponse>> proposalResponseCaptor;

    @BeforeEach
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        peer = testUtils.newMockPeer("peer");
        channel = testUtils.newMockChannel("channel");
        when(channel.sendTransaction(anyCollection(), any(Channel.TransactionOptions.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(channel.getPeers(any())).thenReturn(Collections.singletonList(peer));

        HFClient client = testUtils.newMockClient();
        when(client.getChannel(anyString())).thenReturn(channel);
        when(client.newTransactionProposalRequest()).thenReturn(HFClient.createNewInstance().newTransactionProposalRequest());
        when(client.newQueryProposalRequest()).thenReturn(HFClient.createNewInstance().newQueryProposalRequest());

        commitHandler = mock(CommitHandler.class);
        gateway = TestUtils.getInstance().newGatewayBuilder()
                .client(client)
                .commitHandler((transactionId, network) -> commitHandler)
                .commitTimeout(timeout.getTime(), timeout.getTimeUnit())
                .connect();
        contract = gateway.getNetwork("network").getContract("contract");

        failureResponse = testUtils.newFailureProposalResponse("Epic fail");

        transientMap = new HashMap<>();
    	transientMap.put("key1", "value1".getBytes());
    	transientMap.put("key2", "value2".getBytes());
    }

    @AfterEach
    public void afterEach() {
        gateway.close();
    }

    @Test
    public void testGetName() {
        String name = "txn";
        String result = contract.createTransaction(name).getName();
        assertThat(result).isEqualTo(name);
    }

    @Test
    public void testEvaluateNoResponse() throws Exception {
        ProposalResponse noResponse = testUtils.newUnavailableProposalResponse("No response");
        when(channel.queryByChaincode(any(), anyCollection())).thenReturn(Collections.singletonList(noResponse));

        assertThatThrownBy(() -> contract.evaluateTransaction("txn", "arg1"))
                .isInstanceOf(ContractException.class);
    }

    @Test
    public void testEvaluateUnsuccessfulResponse() throws Exception {
        when(failureResponse.getPeer()).thenReturn(peer);
        when(channel.queryByChaincode(any(), anyCollection())).thenReturn(Collections.singletonList(failureResponse));

        assertThatThrownBy(() -> contract.evaluateTransaction("txn", "arg1"))
                .isInstanceOf(GatewayException.class);
    }

    @Test
    public void testEvaluateSuccess() throws Exception {
        String expected = "successful result";
        ProposalResponse response = testUtils.newSuccessfulProposalResponse(expected.getBytes());
        when(response.getPeer()).thenReturn(peer);
        when(channel.queryByChaincode(any(), anyCollection())).thenReturn(Collections.singletonList(response));

        byte[] result = contract.evaluateTransaction("txn", "arg1");
        assertThat(new String(result)).isEqualTo(expected);
    }

    @Test
    public void testEvaluateSuccessWithTransient() throws Exception {
        String expected = "successful result";
        ProposalResponse response = testUtils.newSuccessfulProposalResponse(expected.getBytes());
        when(response.getPeer()).thenReturn(peer);
        when(channel.queryByChaincode(any(), anyCollection())).thenReturn(Collections.singletonList(response));

        byte[] result = contract.createTransaction("txn").setTransient(transientMap).evaluate("arg1");
        assertThat(new String(result)).isEqualTo(expected);
    }

    @Test
    public void testSubmitNoResponses() throws Exception {
        List<ProposalResponse> responses = new ArrayList<>();
        when(channel.sendTransactionProposal(any())).thenReturn(responses);

        assertThatThrownBy(() -> contract.submitTransaction("txn", "arg1"))
                .isInstanceOf(GatewayException.class);
    }

    @Test
    public void testSubmitUnsuccessfulResponse() throws Exception {
        when(channel.sendTransactionProposal(any())).thenReturn(Collections.singletonList(failureResponse));

        assertThatThrownBy(() -> contract.submitTransaction("txn", "arg1"))
                .isInstanceOf(GatewayException.class);
    }

    @Test
    public void testSubmitSuccess() throws Exception {
        String expected = "successful result";
        ProposalResponse response = testUtils.newSuccessfulProposalResponse(expected.getBytes());
        when(channel.sendTransactionProposal(any())).thenReturn(Collections.singletonList(response));

        byte[] result = contract.submitTransaction("txn", "arg1");
        assertThat(new String(result)).isEqualTo(expected);
    }

    @Test
    public void testSubmitSuccessWithTransient() throws Exception {
        String expected = "successful result";
        ProposalResponse response = testUtils.newSuccessfulProposalResponse(expected.getBytes());
        when(channel.sendTransactionProposal(any())).thenReturn(Collections.singletonList(response));

        byte[] result = contract.createTransaction("txn").setTransient(transientMap).submit("arg1");
        assertThat(new String(result)).isEqualTo(expected);
    }

    @Test
    public void testUsesGatewayCommitTimeout() throws Exception {
        String expected = "successful result";
        ProposalResponse response = testUtils.newSuccessfulProposalResponse(expected.getBytes());
        when(channel.sendTransactionProposal(any())).thenReturn(Collections.singletonList(response));

        contract.submitTransaction("txn", "arg1");

        verify(commitHandler).waitForEvents(timeout.getTime(), timeout.getTimeUnit());
    }

    @Test
    public void testSubmitSuccessWithSomeBadProposalResponses() throws Exception {
        String expected = "successful result";
        ProposalResponse goodResponse = testUtils.newSuccessfulProposalResponse(expected.getBytes());
        when(channel.sendTransactionProposal(any())).thenReturn(Arrays.asList(failureResponse, goodResponse));

        contract.submitTransaction("txn", "arg1");

        verify(channel).sendTransaction(proposalResponseCaptor.capture(), any(Channel.TransactionOptions.class));
        assertThat(proposalResponseCaptor.getValue()).containsOnly(goodResponse);
    }
}
