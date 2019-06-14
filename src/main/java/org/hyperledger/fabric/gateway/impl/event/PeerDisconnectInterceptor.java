/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.event;

import java.util.function.Consumer;

import org.hyperledger.fabric.gateway.spi.PeerDisconnectEvent;
import org.hyperledger.fabric.sdk.Peer;

/**
 * Sets a new disconnect handler for a given peer, which notifies this object's listeners of any disconnect before
 * passing the event to the original handler. Effectively spies on peer disconnects while leaving the original
 * disconnect handling behaviour in place. The original peer disconnect handler is restored when {@link #close()} is
 * called.
 * <p>
 * This implementation is thread-safe.
 * </p>
 */
public final class PeerDisconnectInterceptor implements PeerDisconnectEventSource {
    private final ListenerSet<Consumer<PeerDisconnectEvent>> listeners = new ListenerSet<>();
    private final Peer peer;
    private final Peer.PeerEventingServiceDisconnected disconnectHandler;

    PeerDisconnectInterceptor(Peer peer) {
        this.peer = peer;
        disconnectHandler = peer.getPeerEventingServiceDisconnected();
        peer.setPeerEventingServiceDisconnected(this::handleDisconnect);
    }

    private void handleDisconnect(Peer.PeerEventingServiceDisconnectEvent event) {
        notifyListeners(event);

        // Do this after listener notifications as it may sleep the thread
        disconnectHandler.disconnected(event);
    }

    @Override
    public Consumer<PeerDisconnectEvent> addDisconnectListener(Consumer<PeerDisconnectEvent> listener) {
        return listeners.add(listener);
    }

    @Override
    public void removeDisconnectListener(Consumer<PeerDisconnectEvent> listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(Peer.PeerEventingServiceDisconnectEvent sdkEvent) {
        PeerDisconnectEvent ourEvent = new PeerDisconnectEvent() {
            @Override
            public Peer getPeer() {
                return peer;
            }

            @Override
            public Throwable getCause() {
                return sdkEvent.getExceptionThrown();
            }
        };

        listeners.forEach(listener -> listener.accept(ourEvent));
    }

    @Override
    public void close() {
        listeners.clear();

        // Restore original disconnect handler
        peer.setPeerEventingServiceDisconnected(disconnectHandler);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '@' + System.identityHashCode(this) +
                "(peer=" + peer + ')';
    }
}
