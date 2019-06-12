/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.event;

import org.hyperledger.fabric.gateway.spi.CommitListener;
import org.hyperledger.fabric.gateway.spi.PeerDisconnectEvent;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.Peer;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Adds and removes listeners to transactions commit events (from a given block source) and disconnect events (from given
 * peers).
 */
public final class CommitListenerSession implements ListenerSession {
    private final BlockListenerSession blockListenerSession;
    private final Collection<PeerDisconnectListenerSession> disconnectListenerSessions;

    public CommitListenerSession(BlockEventSource blockSource, CommitListener commitListener, Collection<Peer> peers, String transactionId) {
        Consumer<BlockEvent.TransactionEvent> transactionListener = Listeners.transaction(commitListener, peers, transactionId);
        Consumer<BlockEvent> blockListener = Listeners.fromTransaction(transactionListener);
        blockListenerSession = new BlockListenerSession(blockSource, blockListener);

        Consumer<PeerDisconnectEvent> disconnectListener = commitListener::acceptDisconnect;
        disconnectListenerSessions = peers.stream()
                .map(peer -> new PeerDisconnectListenerSession(peer, disconnectListener))
                .collect(Collectors.toList());
    }

    @Override
    public void close() {
        blockListenerSession.close();
        disconnectListenerSessions.forEach(PeerDisconnectListenerSession::close);
    }
}
