/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.event;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;

/**
 * Used to add and remove block listeners for an underlying Channel.
 * <p>
 * This implementation is thread-safe.
 * </p>
 */
public final class ChannelBlockEventSource implements BlockEventSource {
    private final Map<Consumer<BlockEvent>, String> handleMap = new ConcurrentHashMap<>();
    private final Channel channel;

    ChannelBlockEventSource(Channel channel) {
        this.channel = channel;
    }

    @Override
    public Consumer<BlockEvent> addBlockListener(Consumer<BlockEvent> listener) {
        handleMap.computeIfAbsent(listener, this::registerChannelListener);
        return listener;
    }

    private String registerChannelListener(Consumer<BlockEvent> listener) {
        try {
            return channel.registerBlockListener(listener::accept);
        } catch (InvalidArgumentException e) {
            // Throws if channel has been shutdown
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void removeBlockListener(Consumer<BlockEvent> listener) {
        handleMap.computeIfPresent(listener, (key, value) -> {
            unregisterChannelListener(value);
            return null;
        });
    }

    private void unregisterChannelListener(String handle) {
        try {
            channel.unregisterBlockListener(handle);
        } catch (InvalidArgumentException e) {
            // Ignore to ensure close() never throws
        }
    }

    @Override
    public void close() {
        handleMap.forEach((listener, handle) -> removeBlockListener(listener));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '@' + System.identityHashCode(this) +
                "(channel=" + channel + ')';
    }
}
