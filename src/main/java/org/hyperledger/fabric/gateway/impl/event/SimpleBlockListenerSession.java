/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.event;

import org.hyperledger.fabric.sdk.BlockEvent;

import java.util.function.Consumer;

/**
 * Simply adds and removes the listener from a block event source.
 */
public final class SimpleBlockListenerSession implements ListenerSession {
    private final BlockEventSource blockSource;
    private final Consumer<BlockEvent> listener;

    public SimpleBlockListenerSession(BlockEventSource blockSource, Consumer<BlockEvent> listener) {
        this.blockSource = blockSource;
        this.listener = listener;
        blockSource.addBlockListener(listener);
    }

    @Override
    public void close() {
        blockSource.removeBlockListener(listener);
    }
}
