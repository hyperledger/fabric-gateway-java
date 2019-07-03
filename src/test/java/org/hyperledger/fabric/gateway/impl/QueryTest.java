/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.hyperledger.fabric.gateway.GatewayRuntimeException;
import org.hyperledger.fabric.gateway.TestUtils;
import org.hyperledger.fabric.gateway.spi.Query;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.QueryByChaincodeRequest;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QueryTest {
    private static final TestUtils testUtils = TestUtils.getInstance();

    private Channel channel;
    private Query query;
    private QueryByChaincodeRequest request;
    private Collection<Peer> peers;

    @BeforeEach
    public void beforeEach() {
        channel = testUtils.newMockChannel("channel");
        request = Mockito.mock(QueryByChaincodeRequest.class);
        query = new QueryImpl(channel, request);
        peers = Arrays.asList(testUtils.newMockPeer("peer1"), testUtils.newMockPeer("peer2"));
    }

    @Test
    public void return_query_results_for_multiple_peers() throws Exception {
        Collection<ProposalResponse> responses = peers.stream()
                .map(peer -> {
                    byte[] peerNameBytes = peer.getName().getBytes(StandardCharsets.UTF_8);
                    ProposalResponse response = testUtils.newSuccessfulProposalResponse(peerNameBytes);
                    Mockito.when(response.getPeer()).thenReturn(peer);
                    return response;
                })
                .collect(Collectors.toList());
        Mockito.when(channel.queryByChaincode(request, peers)).thenReturn(responses);

        Map<Peer, ProposalResponse> results = query.evaluate(peers);

        assertThat(results).containsOnlyKeys(peers);
    }

    @Test
    public void return_query_results_for_single_peer() throws Exception {
        Peer peer = peers.iterator().next();
        byte[] peerNameBytes = peer.getName().getBytes(StandardCharsets.UTF_8);
        ProposalResponse response = testUtils.newSuccessfulProposalResponse(peerNameBytes);
        Mockito.when(response.getPeer()).thenReturn(peer);
        Collection<ProposalResponse> responses = Collections.singletonList(response);
        Mockito.when(channel.queryByChaincode(Mockito.any(), Mockito.anyCollection())).thenReturn(responses);

        ProposalResponse result = query.evaluate(peer);

        String payload = new String(result.getChaincodeActionResponsePayload(), StandardCharsets.UTF_8);
        assertThat(payload).isEqualTo(peer.getName());
    }

    @Test
    public void throw_on_bad_proposal_for_multiple_peers() throws Exception {
        Mockito.when(channel.queryByChaincode(request, peers)).thenThrow(ProposalException.class);

        assertThatThrownBy(() -> query.evaluate(peers)).isInstanceOf(GatewayRuntimeException.class);
    }

    @Test
    public void throw_on_bad_proposal_for_single_peer() throws Exception {
        Peer peer = peers.iterator().next();
        Mockito.when(channel.queryByChaincode(Mockito.any(), Mockito.anyCollection())).thenThrow(ProposalException.class);

        assertThatThrownBy(() -> query.evaluate(peer)).isInstanceOf(GatewayRuntimeException.class);
    }
}