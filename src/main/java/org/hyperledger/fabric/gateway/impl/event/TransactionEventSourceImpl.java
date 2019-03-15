/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.event;

import org.hyperledger.fabric.sdk.BlockEvent;

public class TransactionEventSourceImpl implements TransactionEventSource {
    private final BlockEventSource blockSource;
    private final BlockListener blockListener;
    private final ListenerSet<TransactionListener> listeners = new ListenerSet<>();

    public TransactionEventSourceImpl(BlockEventSource blockSource) {
        this.blockSource = blockSource;
        this.blockListener = blockSource.addBlockListener(this::receivedBlock);
    }

    @Override
    public TransactionListener addTransactionListener(TransactionListener listener) {
        return listeners.add(listener);
    }

    @Override
    public void removeTransactionListener(TransactionListener listener) {
        listeners.remove(listener);
    }

    private void receivedBlock(BlockEvent blockEvent) {
        blockEvent.getTransactionEvents().forEach(txEvent -> {
            listeners.forEach(listener -> listener.receivedTransaction(txEvent));
        });
    }

    @Override
    public void close() {
        listeners.clear();
        blockSource.removeBlockListener(blockListener);
    }
}
