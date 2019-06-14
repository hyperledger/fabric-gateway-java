/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.event;

import java.util.function.Consumer;

import org.hyperledger.fabric.sdk.BlockEvent;

/**
 * Simply adds and removes the listener from a block event source.
 */
public final class BlockListenerSession implements ListenerSession {
    private final BlockEventSource blockSource;
    private final Consumer<BlockEvent> listener;

    public BlockListenerSession(BlockEventSource blockSource, Consumer<BlockEvent> listener) {
        this.blockSource = blockSource;
        this.listener = listener;
        blockSource.addBlockListener(listener);
    }

    @Override
    public void close() {
        blockSource.removeBlockListener(listener);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '@' + System.identityHashCode(this) +
                "(blockSource=" + blockSource +
                ", listener=" + listener + ')';
    }
}
