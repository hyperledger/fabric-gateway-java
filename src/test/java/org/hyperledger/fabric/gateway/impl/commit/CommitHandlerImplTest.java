/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.commit;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.hyperledger.fabric.gateway.*;
import org.hyperledger.fabric.gateway.impl.event.StubBlockEventSource;
import org.hyperledger.fabric.gateway.impl.event.StubPeerDisconnectEventSource;
import org.hyperledger.fabric.gateway.spi.CommitHandler;
import org.hyperledger.fabric.gateway.spi.PeerDisconnectEvent;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.exception.TransactionEventException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CommitHandlerImplTest {
    private final TestUtils testUtils = TestUtils.getInstance();
    private final String transactionId = "txId";
    private final long timeout = 10;
    private final TimeUnit timeUnit = TimeUnit.SECONDS;
    private StubBlockEventSource blockSource;
    private Peer peer;
    private Gateway gateway;
    private Network network;
    private CommitHandler commitHandler;
    private CommitStrategy strategy;

    private Map<Peer, StubPeerDisconnectEventSource> peerDisconnectSources = new HashMap<>();

    @BeforeEach
    public void beforeEach() throws Exception {
        blockSource = new StubBlockEventSource();

        peer = testUtils.newMockPeer("peer");
        peerDisconnectSources.put(peer, new StubPeerDisconnectEventSource(peer));

        Collection<Peer> peers = Collections.singletonList(peer);

        gateway = testUtils.newGatewayBuilder().connect();
        network = gateway.getNetwork("ch1");

        strategy = mock(CommitStrategy.class);
        when(strategy.getPeers()).thenReturn(peers);

        commitHandler = new CommitHandlerImpl(transactionId, network, strategy);
    }

    @AfterEach
    public void afterEach() {
        blockSource.close();
        peerDisconnectSources.values().forEach(StubPeerDisconnectEventSource::close);
        peerDisconnectSources.clear();
        commitHandler.cancelListening();
        gateway.close();
    }

    private PeerDisconnectEvent sendPeerDisconnectEvent() {
        StubPeerDisconnectEventSource disconnectSource = peerDisconnectSources.get(peer);
        if (disconnectSource == null) {
            throw new IllegalArgumentException("No disconnect source for peer: " + peer.getName());
        }
        PeerDisconnectEvent event = testUtils.newPeerDisconnectedEvent(peer);
        disconnectSource.sendEvent(event);
        return event;
    }

    private BlockEvent.TransactionEvent sendValidTransactionEvent() {
        return sendValidTransactionEvent(transactionId);
    }

    private BlockEvent.TransactionEvent sendValidTransactionEvent(String transactionId) {
        BlockEvent.TransactionEvent txEvent = testUtils.newValidMockTransactionEvent(peer, transactionId);
        BlockEvent blockEvent = testUtils.newMockBlockEvent(peer, 1, txEvent);
        blockSource.sendEvent(blockEvent);
        return txEvent;
    }

    private BlockEvent.TransactionEvent sendInvalidTransactionEvent() {
        BlockEvent.TransactionEvent txEvent = testUtils.newInvalidMockTransactionEvent(peer, transactionId);
        BlockEvent blockEvent = testUtils.newMockBlockEvent(peer, 1, txEvent);
        blockSource.sendEvent(blockEvent);
        return txEvent;
    }

    @Test
    public void passes_valid_event_to_strategy() {
        when(strategy.onEvent(any())).thenReturn(CommitStrategy.Result.CONTINUE);

        commitHandler.startListening();
        BlockEvent.TransactionEvent event = sendValidTransactionEvent();

        verify(strategy).onEvent(event);
    }

    @Test
    public void passes_disconnect_to_strategy() {
        when(strategy.onError(any())).thenReturn(CommitStrategy.Result.CONTINUE);

        commitHandler.startListening();
        PeerDisconnectEvent event = sendPeerDisconnectEvent();

        verify(strategy).onError(event);
    }

    @Test
    public void does_not_pass_event_for_other_transaction_to_strategy() {
        when(strategy.onEvent(any())).thenReturn(CommitStrategy.Result.CONTINUE);

        commitHandler.startListening();
        sendValidTransactionEvent("bad " + transactionId);

        verify(strategy, never()).onEvent(any());
    }

    @Test
    public void does_not_pass_duplicate_peer_event_to_strategy() {
        when(strategy.onEvent(any())).thenReturn(CommitStrategy.Result.CONTINUE);

        commitHandler.startListening();
        sendValidTransactionEvent();
        sendValidTransactionEvent();

        verify(strategy, times(1)).onEvent(any());
    }

    @Test
    public void does_not_pass_duplicate_peer_disconnect_to_strategy() {
        when(strategy.onEvent(any())).thenReturn(CommitStrategy.Result.CONTINUE);

        commitHandler.startListening();
        sendPeerDisconnectEvent();
        sendPeerDisconnectEvent();

        verify(strategy, times(1)).onError(any());
    }

    @Test
    public void does_not_pass_invalid_event_to_strategy() {
        when(strategy.onEvent(any())).thenReturn(CommitStrategy.Result.CONTINUE);

        commitHandler.startListening();
        sendInvalidTransactionEvent();

        verify(strategy, never()).onEvent(any());
    }

    @Test
    public void wait_returns_if_peer_commit_causes_strategy_success() throws Exception {
        when(strategy.onEvent(any())).thenReturn(CommitStrategy.Result.SUCCESS);

        commitHandler.startListening();
        sendValidTransactionEvent();
        commitHandler.waitForEvents(timeout, timeUnit);
    }

    @Test
    public void wait_returns_if_peer_disconnect_causes_strategy_success() throws Exception {
        when(strategy.onError(any())).thenReturn(CommitStrategy.Result.SUCCESS);

        commitHandler.startListening();
        sendPeerDisconnectEvent();
        commitHandler.waitForEvents(timeout, timeUnit);
    }

    @Test
    public void wait_returns_if_peer_commit_causes_strategy_fail() {
        when(strategy.onEvent(any())).thenReturn(CommitStrategy.Result.FAIL);

        commitHandler.startListening();
        sendValidTransactionEvent();
        assertThatThrownBy(() -> commitHandler.waitForEvents(timeout, timeUnit))
                .isInstanceOf(GatewayException.class);
    }

    @Test
    public void wait_throws_contract_exception_caused_by_transaction_event_exception_if_peer_commit_fails() {
        when(strategy.onEvent(any())).thenReturn(CommitStrategy.Result.CONTINUE);

        commitHandler.startListening();
        sendInvalidTransactionEvent();
        assertThatThrownBy(() -> commitHandler.waitForEvents(timeout, timeUnit))
                .isInstanceOf(ContractException.class)
                .hasRootCauseExactlyInstanceOf(TransactionEventException.class);
    }

    @Test
    public void wait_returns_if_cancelled() {
        commitHandler.startListening();
        commitHandler.cancelListening();
        assertThatCode(() -> commitHandler.waitForEvents(timeout, timeUnit))
                .doesNotThrowAnyException();
    }

    @Test
    public void wait_throws_on_timeout() {
        commitHandler.startListening();
        assertThatThrownBy(() -> commitHandler.waitForEvents(1, TimeUnit.NANOSECONDS))
                .isInstanceOf(TimeoutException.class);
    }

    @Test
    public void wait_returns_if_no_peers() {
        strategy = mock(CommitStrategy.class);
        when(strategy.getPeers()).thenReturn(Collections.emptyList());
        commitHandler = new CommitHandlerImpl(transactionId, network, strategy);

        commitHandler.startListening();
        assertThatCode(() -> commitHandler.waitForEvents(timeout, timeUnit))
                .doesNotThrowAnyException();
    }
}
