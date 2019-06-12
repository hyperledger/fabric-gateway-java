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
 * Stub implementation of a PeerDisconnectEventSource to allow tests to drive events into the system.
 * <p>
 * Creating an getInstance of this class modifies the behaviour of the {@link PeerDisconnectEventSourceFactory} so that
 * this getInstance is always returned by the factory for its associated peer. It is important to call {@link #close()}
 * to restore default factory behaviour.
 * </p>
 */
public class StubPeerDisconnectEventSource implements PeerDisconnectEventSource {
    private final ListenerSet<Consumer<PeerDisconnectEvent>> listeners = new ListenerSet<>();
    private final Peer peer;

    public StubPeerDisconnectEventSource(Peer peer) {
        this.peer = peer;
        PeerDisconnectEventSourceFactory.getInstance().setPeerDisconnectEventSource(peer, this);
    }

    @Override
    public Consumer<PeerDisconnectEvent> addDisconnectListener(Consumer<PeerDisconnectEvent> listener) {
        return listeners.add(listener);
    }

    @Override
    public void removeDisconnectListener(Consumer<PeerDisconnectEvent> listener) {
        listeners.remove(listener);
    }

    @Override
    public void close() {
        listeners.clear();
        PeerDisconnectEventSourceFactory.getInstance().setPeerDisconnectEventSource(peer, null);
    }

    public void sendEvent(PeerDisconnectEvent event) {
        listeners.forEach(listener -> listener.accept(event));
    }
}
