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
 * Stub {@link Checkpointer} for testing. Essentially an in-memory checkpointer implementation with no persistent
 * storage.
 */
public class StubCheckpointer implements Checkpointer {
    private long blockNumber = Checkpointer.UNSET_BLOCK_NUMBER;
    private Set<String> transactionIds = new HashSet<>();

    @Override
    public long getBlockNumber() {
        return blockNumber;
    }

    @Override
    public void setBlockNumber(long blockNumber) {
        this.blockNumber = blockNumber;
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
    public void delete() {
        blockNumber = Checkpointer.UNSET_BLOCK_NUMBER;
        transactionIds.clear();
    }

    @Override
    public void close() { }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(blockNumber=" + blockNumber +
                ", transactionIds=" + transactionIds + ")";
    }
}
