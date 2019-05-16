/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.event;

import org.hyperledger.fabric.sdk.BlockEvent;

import java.util.function.Consumer;

/**
 * Allows observing received transaction events.
 */
public interface TransactionEventSource extends AutoCloseable {
    Consumer<BlockEvent.TransactionEvent> addTransactionListener(Consumer<BlockEvent.TransactionEvent> listener);
    void removeTransactionListener(Consumer<BlockEvent.TransactionEvent> listener);

    @Override
    void close();
}
