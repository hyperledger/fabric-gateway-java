/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.event;

import org.hyperledger.fabric.gateway.spi.PeerDisconnectEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

/**
 * Attaches a single listener to multiple peer disconnect event sources. Instances should be closed after use by
 * calling {@link #close()}, which will detach the listener from event sources.
 * <p>
 * This implementation is thread-safe.
 * </p>
 */
public final class CompositePeerDisconnectListener implements AutoCloseable {
    private final Collection<PeerDisconnectEventSource> eventSources;
    private final Consumer<PeerDisconnectEvent> listener;

    public CompositePeerDisconnectListener(Consumer<PeerDisconnectEvent> listener, Collection<PeerDisconnectEventSource> eventSources) {
        this.eventSources = new ArrayList<>(eventSources);
        this.listener = listener;

        this.eventSources.forEach(eventSource -> eventSource.addDisconnectListener(listener));
    }

    public synchronized void close() {
        eventSources.forEach(eventSource -> eventSource.removeDisconnectListener(listener));
        eventSources.clear();
    }
}
