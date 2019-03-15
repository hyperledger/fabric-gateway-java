/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.event;

/**
 * Allows observing received transaction events.
 */
public interface TransactionEventSource extends AutoCloseable {
    TransactionListener addTransactionListener(TransactionListener listener);
    void removeTransactionListener(TransactionListener listener);

    @Override
    void close();
}
