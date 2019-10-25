/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.event;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.hyperledger.fabric.sdk.Peer;

/**
 * Factory for creating PeerDisconnectEventSource instances for a given peer.
 */
public final class PeerDisconnectEventSourceFactory {
    private static final PeerDisconnectEventSourceFactory INSTANCE = new PeerDisconnectEventSourceFactory();

    // Use a weak map to avoid holding on to peers once they are out of scope elsewhere
    private final Map<Peer, PeerDisconnectEventSource> eventSources = Collections.synchronizedMap(new WeakHashMap<>());

    public static PeerDisconnectEventSourceFactory getInstance() {
        return INSTANCE;
    }

    private PeerDisconnectEventSourceFactory() { }

    /**
     * Get the event source for a given peer.
     * @return Event source.
     */
    public PeerDisconnectEventSource getPeerDisconnectEventSource(final Peer peer) {
        return eventSources.computeIfAbsent(peer, key -> new PeerDisconnectInterceptor(peer));
    }

    /**
     * Used only for testing. Sets the event source that will be returned by this factory for a given peer.
     * @param peer A peer.
     * @param eventSource An event source, or {@code null}.
     */
    void setPeerDisconnectEventSource(final Peer peer, final PeerDisconnectEventSource eventSource) {
        PeerDisconnectEventSource previousValue = eventSources.put(peer, eventSource);
        if (previousValue != null) {
            previousValue.close();
        }
    }
}
