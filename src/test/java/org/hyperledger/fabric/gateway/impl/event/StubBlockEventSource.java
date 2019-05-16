/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.event;

import org.hyperledger.fabric.sdk.BlockEvent;

import java.util.function.Consumer;

/**
 * Stub implementation of a BlockEventSource to allow tests to drive events into the system.
 *
 * <p>Creating an instance of this class modifies the behaviour of the {@link BlockEventSourceFactory} so that this
 * instance is always returned by the factory. It is important to call {@link #close()} to restore default factory
 * behaviour.</p>
 */
public class StubBlockEventSource implements BlockEventSource {
    private final ListenerSet<Consumer<BlockEvent>> listeners = new ListenerSet<>();

    public StubBlockEventSource() {
        BlockEventSourceFactory.setFactoryFunction(channel -> this);
    }

    @Override
    public Consumer<BlockEvent> addBlockListener(Consumer<BlockEvent> listener) {
        return listeners.add(listener);
    }

    @Override
    public void removeBlockListener(Consumer<BlockEvent> listener) {
        listeners.remove(listener);
    }

    @Override
    public void close() {
        listeners.clear();
        BlockEventSourceFactory.setFactoryFunction(BlockEventSourceFactory.DEFAULT_FACTORY_FN);
    }

    public void sendEvent(BlockEvent event) {
        listeners.forEach(listener -> listener.accept(event));
    }
}
