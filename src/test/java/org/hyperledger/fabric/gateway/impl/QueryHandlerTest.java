/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

import org.hyperledger.fabric.gateway.ContractException;
import org.hyperledger.fabric.gateway.TestUtils;
import org.hyperledger.fabric.gateway.spi.Query;
import org.hyperledger.fabric.gateway.spi.QueryHandler;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public final class QueryHandlerTest {
    @FunctionalInterface
    private interface HandlerConstructor extends Function<Collection<Peer>, QueryHandler> { }

    private enum HandlerType implements HandlerConstructor {
        SINGLE(peers -> new SingleQueryHandler(peers)),
        ROUND_ROBIN(peers -> new RoundRobinQueryHandler(peers));

        private final HandlerConstructor constructor;

        HandlerType(HandlerConstructor constructor) {
            this.constructor = constructor;
        }

        @Override
        public QueryHandler apply(Collection<Peer> peers) {
            return constructor.apply(peers);
        }
    }

    private final TestUtils testUtils = TestUtils.getInstance();
    private Peer peer1;
    private Peer peer2;
    private ProposalResponse successfulResponse;
    private ProposalResponse failureResponse;
    private ProposalResponse unavailableResponse;

    @BeforeEach
    public void beforeEach() {
        peer1 = testUtils.newMockPeer("peer1");
        peer2 = testUtils.newMockPeer("peer2");
        successfulResponse = testUtils.newSuccessfulProposalResponse(new byte[0]);
        failureResponse = testUtils.newFailureProposalResponse("Epic fail");
        unavailableResponse = testUtils.newUnavailableProposalResponse("No response from peer");
    }

    @ParameterizedTest
    @EnumSource(HandlerType.class)
    public void throws_if_no_peers_supplied(HandlerConstructor constructor) {
        assertThatThrownBy(() -> constructor.apply(Collections.emptyList()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @EnumSource(HandlerType.class)
    public void returns_successful_peer_response(HandlerConstructor constructor) throws ContractException {
        Query query = mock(Query.class);
        when(query.evaluate(peer1)).thenReturn(successfulResponse);

        QueryHandler handler = constructor.apply(Collections.singletonList(peer1));
        ProposalResponse result = handler.evaluate(query);

        assertThat(result).isEqualTo(successfulResponse);
    }

    @ParameterizedTest
    @EnumSource(HandlerType.class)
    public void throws_on_failure_peer_response(HandlerConstructor constructor) {
        Query query = mock(Query.class);
        when(query.evaluate(any(Peer.class)))
                .thenReturn(failureResponse)
                .thenReturn(successfulResponse);

        QueryHandler handler = constructor.apply(Arrays.asList(peer1, peer2));
        ContractException e = catchThrowableOfType(() -> handler.evaluate(query), ContractException.class);

        assertThat(e).hasMessageContaining(failureResponse.getMessage());
        assertThat(e.getProposalResponses()).containsExactly(failureResponse);
    }

    @ParameterizedTest
    @EnumSource(HandlerType.class)
    public void throws_if_all_peers_are_unavailable(HandlerConstructor constructor) {
        Query query = mock(Query.class);
        when(query.evaluate(peer1)).thenReturn(unavailableResponse);

        QueryHandler handler = constructor.apply(Collections.singletonList(peer1));
        ContractException e = catchThrowableOfType(() -> handler.evaluate(query), ContractException.class);

        assertThat(e).hasMessageContaining(unavailableResponse.getMessage());
        assertThat(e.getProposalResponses()).containsExactly(unavailableResponse);
    }

    @ParameterizedTest
    @EnumSource(HandlerType.class)
    public void failover_on_peer_failure(HandlerConstructor constructor) throws ContractException {
        Query query = mock(Query.class);
        when(query.evaluate(any(Peer.class)))
                .thenReturn(unavailableResponse)
                .thenReturn(successfulResponse);

        QueryHandler handler = constructor.apply(Arrays.asList(peer1, peer2));
        handler.evaluate(query);

        verify(query, times(2)).evaluate(any(Peer.class));
    }

    @ParameterizedTest
    @EnumSource(value = HandlerType.class, names = { "SINGLE" })
    public void same_peer_for_two_successful_queries(HandlerConstructor constructor) throws ContractException {
        Query query = mock(Query.class);
        when(query.evaluate(any(Peer.class))).thenReturn(successfulResponse);

        QueryHandler handler = constructor.apply(Arrays.asList(peer1, peer2));
        handler.evaluate(query);
        handler.evaluate(query);

        ArgumentCaptor<Peer> argument = ArgumentCaptor.forClass(Peer.class);
        verify(query, times(2)).evaluate(argument.capture());
        Peer arg1 = argument.getAllValues().get(0);
        Peer arg2 = argument.getAllValues().get(1);
        assertThat(arg1).isSameAs(arg2);
    }

    @ParameterizedTest
    @EnumSource(value = HandlerType.class, names = { "ROUND_ROBIN" })
    public void different_peers_for_two_successful_queries(HandlerConstructor constructor) throws ContractException {
        Query query = mock(Query.class);
        when(query.evaluate(any(Peer.class))).thenReturn(successfulResponse);

        QueryHandler handler = constructor.apply(Arrays.asList(peer1, peer2));
        handler.evaluate(query);
        handler.evaluate(query);

        ArgumentCaptor<Peer> argument = ArgumentCaptor.forClass(Peer.class);
        verify(query, times(2)).evaluate(argument.capture());
        Peer arg1 = argument.getAllValues().get(0);
        Peer arg2 = argument.getAllValues().get(1);
        assertThat(arg1).isNotSameAs(arg2);
    }
}