/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import org.hyperledger.fabric.gateway.spi.Checkpointer;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Transient in-memory checkpointer implementation with no persistent storage. Can be used for event replay.
 */
public class InMemoryCheckpointer implements Checkpointer {
    private long blockNumber;
    private final Set<String> transactionIds = new HashSet<>();

    public InMemoryCheckpointer() {
        this(Checkpointer.UNSET_BLOCK_NUMBER);
    }

    public InMemoryCheckpointer(long blockNumber) {
        this.blockNumber = blockNumber;
    }

    @Override
    public synchronized long getBlockNumber() {
        return blockNumber;
    }

    @Override
    public void setBlockNumber(long blockNumber) {
        this.blockNumber = blockNumber;
        this.transactionIds.clear();
    }

    @Override
    public Set<String> getTransactionIds() {
        return Collections.unmodifiableSet(transactionIds);
    }

    @Override
    public void addTransactionId(String transactionId) {
        transactionIds.add(transactionId);
    }

    @Override
    public void close() { }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(blockNumber=" + blockNumber +
                ", transactionIds=" + transactionIds + ")";
    }
}
