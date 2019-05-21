/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.event;

import org.hyperledger.fabric.sdk.BlockEvent;

import java.util.function.Consumer;

public final class TransactionEventSourceImpl implements TransactionEventSource {
    private final BlockEventSource blockSource;
    private final Consumer<BlockEvent> blockListener;
    private final ListenerSet<Consumer<BlockEvent.TransactionEvent>> listeners = new ListenerSet<>();

    public TransactionEventSourceImpl(BlockEventSource blockSource) {
        this.blockSource = blockSource;
        this.blockListener = blockSource.addBlockListener(this::receivedBlock);
    }

    @Override
    public Consumer<BlockEvent.TransactionEvent> addTransactionListener(Consumer<BlockEvent.TransactionEvent> listener) {
        return listeners.add(listener);
    }

    @Override
    public void removeTransactionListener(Consumer<BlockEvent.TransactionEvent> listener) {
        listeners.remove(listener);
    }

    private void receivedBlock(BlockEvent blockEvent) {
        blockEvent.getTransactionEvents().forEach(txEvent -> {
            listeners.forEach(listener -> listener.accept(txEvent));
        });
    }

    @Override
    public void close() {
        listeners.clear();
        blockSource.removeBlockListener(blockListener);
    }
}
