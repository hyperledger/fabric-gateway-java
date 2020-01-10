/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.query;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.hyperledger.fabric.gateway.ContractException;
import org.hyperledger.fabric.gateway.TestUtils;
import org.hyperledger.fabric.gateway.spi.Query;
import org.hyperledger.fabric.gateway.spi.QueryHandler;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public abstract class CommonQueryHandlerTest {
    protected final TestUtils testUtils = TestUtils.getInstance();
    protected Peer peer1;
    protected Peer peer2;
    protected ProposalResponse successfulResponse;
    protected ProposalResponse failureResponse;
    protected ProposalResponse unavailableResponse;
    
    public abstract QueryHandler newQueryHandler(Collection<Peer> peers);

    @BeforeEach
    public void beforeEach() {
        peer1 = testUtils.newMockPeer("peer1");
        peer2 = testUtils.newMockPeer("peer2");
        successfulResponse = testUtils.newSuccessfulProposalResponse(new byte[0]);
        failureResponse = testUtils.newFailureProposalResponse("Epic fail");
        unavailableResponse = testUtils.newUnavailableProposalResponse("No response from peer");
    }

    @Test
    public void throws_if_no_peers_supplied() {
        assertThatThrownBy(() -> newQueryHandler(Collections.emptyList()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void returns_successful_peer_response() throws ContractException {
        Query query = mock(Query.class);
        when(query.evaluate(peer1)).thenReturn(successfulResponse);

        QueryHandler handler = newQueryHandler(Collections.singletonList(peer1));
        ProposalResponse result = handler.evaluate(query);

        assertThat(result).isEqualTo(successfulResponse);
    }

    @Test
    public void throws_on_failure_peer_response() {
        Query query = mock(Query.class);
        when(query.evaluate(any(Peer.class)))
                .thenReturn(failureResponse)
                .thenReturn(successfulResponse);

        QueryHandler handler = newQueryHandler(Arrays.asList(peer1, peer2));
        ContractException e = catchThrowableOfType(() -> handler.evaluate(query), ContractException.class);

        assertThat(e).hasMessageContaining(failureResponse.getMessage());
        assertThat(e.getProposalResponses()).containsExactly(failureResponse);
    }

    @Test
    public void throws_if_all_peers_are_unavailable() {
        Query query = mock(Query.class);
        when(query.evaluate(peer1)).thenReturn(unavailableResponse);

        QueryHandler handler = newQueryHandler(Collections.singletonList(peer1));
        ContractException e = catchThrowableOfType(() -> handler.evaluate(query), ContractException.class);

        assertThat(e).hasMessageContaining(unavailableResponse.getMessage());
        assertThat(e.getProposalResponses()).containsExactly(unavailableResponse);
    }

    @Test
    public void failover_on_peer_failure() throws ContractException {
        Query query = mock(Query.class);
        when(query.evaluate(any(Peer.class)))
                .thenReturn(unavailableResponse)
                .thenReturn(successfulResponse);

        QueryHandler handler = newQueryHandler(Arrays.asList(peer1, peer2));
        handler.evaluate(query);

        verify(query, times(2)).evaluate(any(Peer.class));
    }
}
