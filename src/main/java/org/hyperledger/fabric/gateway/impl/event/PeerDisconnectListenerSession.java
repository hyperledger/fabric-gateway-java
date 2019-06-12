/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.event;

import org.hyperledger.fabric.gateway.spi.PeerDisconnectEvent;
import org.hyperledger.fabric.sdk.Peer;

import java.util.function.Consumer;

/**
 * Simply adds and removes the listener from a peer disconnect event source.
 */
public final class PeerDisconnectListenerSession implements ListenerSession {
    private final PeerDisconnectEventSource disconnectSource;
    private final Consumer<PeerDisconnectEvent> listener;

    public PeerDisconnectListenerSession(Peer peer, Consumer<PeerDisconnectEvent> listener) {
        disconnectSource = PeerDisconnectEventSourceFactory.getInstance().getPeerDisconnectEventSource(peer);
        this.listener = listener;
        disconnectSource.addDisconnectListener(listener);
    }

    @Override
    public void close() {
        disconnectSource.removeDisconnectListener(listener);
    }
}
