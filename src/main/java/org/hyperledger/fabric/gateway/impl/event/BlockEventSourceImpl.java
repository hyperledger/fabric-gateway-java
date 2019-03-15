/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.event;

import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Used to add and remove block listeners for an underlying Channel.
 * <p>
 * This implementation is thread-safe.
 * </p>
 */
public final class BlockEventSourceImpl implements BlockEventSource {
    private final Map<BlockListener, String> handleMap = new ConcurrentHashMap<>();
    private final Channel channel;

    BlockEventSourceImpl(Channel channel) {
        this.channel = channel;
    }

    @Override
    public BlockListener addBlockListener(BlockListener listener) {
        handleMap.computeIfAbsent(listener, this::registerChannelListener);
        return listener;
    }

    private String registerChannelListener(BlockListener listener) {
        try {
            return channel.registerBlockListener(listener::receivedBlock);
        } catch (InvalidArgumentException e) {
            // Throws if channel has been shutdown
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void removeBlockListener(BlockListener listener) {
        handleMap.computeIfPresent(listener, (key, value) -> {
            unregisterChannelListener(value);
            return null;
        });
    }

    private void unregisterChannelListener(String handle) {
        try {
            channel.unregisterBlockListener(handle);
        } catch (InvalidArgumentException e) {
            // Throws if channel has been shutdown
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void close() {
        handleMap.forEach((listener, handle) -> removeBlockListener(listener));
    }
}
