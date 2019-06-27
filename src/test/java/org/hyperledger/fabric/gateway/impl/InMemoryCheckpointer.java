/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.hyperledger.fabric.gateway.spi.Checkpointer;

/**
 * Transient in-memory checkpointer implementation with no persistent storage.
 */
public class InMemoryCheckpointer implements Checkpointer {
    private final AtomicLong blockNumber;
    private final Set<String> transactionIds = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public InMemoryCheckpointer() {
        this(Checkpointer.UNSET_BLOCK_NUMBER);
    }

    public InMemoryCheckpointer(long startBlock) {
        blockNumber = new AtomicLong(startBlock);
    }

    @Override
    public long getBlockNumber() {
        return blockNumber.get();
    }

    @Override
    public synchronized void setBlockNumber(long blockNumber) {
        this.blockNumber.set(blockNumber);
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
        return getClass().getSimpleName() + '@' + System.identityHashCode(this) +
                "(blockNumber=" + blockNumber.get() +
                ", transactionIds=" + transactionIds + ')';
    }
}
