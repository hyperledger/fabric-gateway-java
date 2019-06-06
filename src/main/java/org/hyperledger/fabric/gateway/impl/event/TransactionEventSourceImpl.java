/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.event;

import org.hyperledger.fabric.sdk.BlockEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class TransactionEventSourceImpl implements TransactionEventSource {
    private final BlockEventSource blockSource;
    private final Map<Consumer<BlockEvent.TransactionEvent>, ListenerSession> listenerSessions = new ConcurrentHashMap<>();

    public TransactionEventSourceImpl(BlockEventSource blockSource) {
        this.blockSource = blockSource;
    }

    @Override
    public Consumer<BlockEvent.TransactionEvent> addTransactionListener(Consumer<BlockEvent.TransactionEvent> listener) {
        listenerSessions.computeIfAbsent(listener, k -> newListenerSession(listener));
        return listener;
    }

    private ListenerSession newListenerSession(Consumer<BlockEvent.TransactionEvent> listener) {
        return new BlockListenerSession(blockSource, blockEvent -> {
            blockEvent.getTransactionEvents().forEach(listener);
        });
    }

    @Override
    public void removeTransactionListener(Consumer<BlockEvent.TransactionEvent> listener) {
        ListenerSession session = listenerSessions.remove(listener);
        if (session != null) {
            session.close();
        }
    }

    @Override
    public void close() {
        listenerSessions.forEach((listener, session) -> session.close());
        listenerSessions.clear();
    }
}
