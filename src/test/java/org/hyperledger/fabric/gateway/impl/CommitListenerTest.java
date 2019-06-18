/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.TestUtils;
import org.hyperledger.fabric.gateway.impl.event.StubBlockEventSource;
import org.hyperledger.fabric.gateway.impl.event.StubPeerDisconnectEventSource;
import org.hyperledger.fabric.gateway.spi.CommitListener;
import org.hyperledger.fabric.gateway.spi.PeerDisconnectEvent;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.Peer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class CommitListenerTest {
    private static final TestUtils testUtils = TestUtils.getInstance();

    private Gateway gateway;
    private Network network;
    private StubBlockEventSource stubBlockEventSource;
    private StubPeerDisconnectEventSource stubPeer1DisconnectEventSource;
    private StubPeerDisconnectEventSource stubPeer2DisconnectEventSource;
    private final Peer peer1 = testUtils.newMockPeer("peer1");
    private final Peer peer2 = testUtils.newMockPeer("peer2");
    private final Set<Peer> peers = Stream.of(peer1, peer2).collect(Collectors.toSet());
    private final String transactionId = "txId";
    private final CommitListener stubCommitListener = new CommitListener() {
        @Override
        public void acceptCommit(BlockEvent.TransactionEvent transactionEvent) { }

        @Override
        public void acceptDisconnect(PeerDisconnectEvent disconnectEvent) { }
    };

    @BeforeEach
    public void beforeEach() throws Exception {
        stubBlockEventSource = new StubBlockEventSource(); // Must be before network is created
        stubPeer1DisconnectEventSource = new StubPeerDisconnectEventSource(peer1);
        stubPeer2DisconnectEventSource = new StubPeerDisconnectEventSource(peer2);
        gateway = testUtils.newGatewayBuilder().connect();
        network = gateway.getNetwork("ch1");
    }

    @AfterEach
    public void afterEach() {
        stubBlockEventSource.close();
        stubPeer1DisconnectEventSource.close();
        stubPeer2DisconnectEventSource.close();
        gateway.close();
    }

    private void fireCommitEvents(Peer peer, String... transactionIds) {
        List<BlockEvent.TransactionEvent> transactionEvents = Arrays.stream(transactionIds)
                .map(transactionId -> testUtils.newValidMockTransactionEvent(peer, transactionId))
                .collect(Collectors.toList());
        BlockEvent blockEvent = testUtils.newMockBlockEvent(peer, 1, transactionEvents);
        stubBlockEventSource.sendEvent(blockEvent);
    }

    @Test
    public void add_listener_returns_the_listener() {
        CommitListener result = network.addCommitListener(stubCommitListener, peers, transactionId);

        assertThat(result).isSameAs(stubCommitListener);
    }

    @Test
    public void listener_only_receives_commits_for_correct_transaction_id() {
        CommitListener listener = spy(stubCommitListener);

        network.addCommitListener(listener, peers, transactionId);
        fireCommitEvents(peer1, transactionId, "BAD_" + transactionId);

        verify(listener, times(1)).acceptCommit(any(BlockEvent.TransactionEvent.class));
    }

    @Test
    public void listener_only_receives_commits_for_correct_peers() {
        CommitListener listener = spy(stubCommitListener);

        network.addCommitListener(listener, Collections.singleton(peer1), transactionId);
        fireCommitEvents(peer1, transactionId);
        fireCommitEvents(peer2, transactionId);

        verify(listener, times(1)).acceptCommit(any(BlockEvent.TransactionEvent.class));
    }

    @Test
    public void removed_listener_does_not_receive_commit_events() {
        CommitListener listener = spy(stubCommitListener);

        network.addCommitListener(listener, peers, transactionId);
        network.removeCommitListener(listener);
        fireCommitEvents(peer1, transactionId);

        verify(listener, never()).acceptCommit(any(BlockEvent.TransactionEvent.class));
    }

    @Test
    public void listener_only_receives_disconnects_for_specified_peers() {
        CommitListener listener = spy(stubCommitListener);
        PeerDisconnectEvent peer1Event = testUtils.newPeerDisconnectedEvent(peer1);
        PeerDisconnectEvent peer2Event = testUtils.newPeerDisconnectedEvent(peer2);

        network.addCommitListener(listener, Collections.singleton(peer1), transactionId);
        stubPeer1DisconnectEventSource.sendEvent(peer1Event);
        stubPeer2DisconnectEventSource.sendEvent(peer2Event);

        verify(listener, times(1)).acceptDisconnect(any(PeerDisconnectEvent.class));
    }

    @Test
    public void removed_listener_does_not_receive_disconnect_events() {
        CommitListener listener = spy(stubCommitListener);
        PeerDisconnectEvent peer1Event = testUtils.newPeerDisconnectedEvent(peer1);

        network.addCommitListener(listener, peers, transactionId);
        network.removeCommitListener(listener);
        stubPeer1DisconnectEventSource.sendEvent(peer1Event);

        verify(listener, never()).acceptDisconnect(any(PeerDisconnectEvent.class));
    }

    @Test
    public void close_network_removes_listeners() {
        CommitListener listener = spy(stubCommitListener);
        PeerDisconnectEvent peer1Event = testUtils.newPeerDisconnectedEvent(peer1);

        network.addCommitListener(listener, peers, transactionId);
        ((NetworkImpl)network).close();
        fireCommitEvents(peer1, transactionId);
        stubPeer1DisconnectEventSource.sendEvent(peer1Event);

        verify(listener, never()).acceptCommit(any(BlockEvent.TransactionEvent.class));
        verify(listener, never()).acceptDisconnect(any(PeerDisconnectEvent.class));
    }
}
