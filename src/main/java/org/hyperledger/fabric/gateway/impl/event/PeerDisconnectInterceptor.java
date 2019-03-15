/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.event;

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
    private final ListenerSet<PeerDisconnectListener> listeners = new ListenerSet<>();
    private final Peer peer;
    private final Peer.PeerEventingServiceDisconnected disconnectHandler;
    private final Object initLock = new Object();

    PeerDisconnectInterceptor(Peer peer) {
        this.peer = peer;
        synchronized (initLock) {
            disconnectHandler = peer.setPeerEventingServiceDisconnected(this::handleDisconnect);
        }
    }

    private void handleDisconnect(Peer.PeerEventingServiceDisconnectEvent event) {
        notifyListeners(event);

        final Peer.PeerEventingServiceDisconnected handler;
        synchronized (initLock) {
            handler = disconnectHandler;
        }
        // Do this after listener notifications as it may sleep the thread
        handler.disconnected(event);
    }

    @Override
    public PeerDisconnectListener addDisconnectListener(PeerDisconnectListener listener) {
        return listeners.add(listener);
    }

    @Override
    public void removeDisconnectListener(PeerDisconnectListener listener) {
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

        listeners.forEach(listener -> listener.peerDisconnected(ourEvent));
    }

    @Override
    public void close() {
        listeners.clear();
        synchronized (initLock) {
            // Restore original disconnect handler
            peer.setPeerEventingServiceDisconnected(disconnectHandler);
        }
    }
}
