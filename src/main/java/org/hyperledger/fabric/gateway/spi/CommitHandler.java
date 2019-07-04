/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.spi;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.hyperledger.fabric.gateway.ContractException;
import org.hyperledger.fabric.gateway.Network;

/**
 * Class to handle waiting for an appropriate number of successful commit events to be received from peers following
 * submit of a transaction to the orderer. Each handler instance will handle a single transaction so can maintain
 * instance state related to events received during its lifetime.
 * <p>Implementations may use {@link Network#addCommitListener(CommitListener, Collection, String)} to identify
 * when specific peers have committed the transaction they are tracking.</p>
 */
public interface CommitHandler {
    /**
     * Called to initiate listening for transaction events.
     */
    void startListening();

    /**
     * Block until enough transaction commit events have been received to satisfy the event handling strategy.
     * @param timeout the maximum time to wait.
     * @param timeUnit the time unit of the timeout argument.
     * @throws ContractException if the commit fails, either by being rejected by a peer of failing to meet the
     * requirements of the strategy.
     * @throws TimeoutException if the strategy was not satisfied in time.
     */
    void waitForEvents(long timeout, TimeUnit timeUnit) throws ContractException, TimeoutException;

    /**
     * Called to interrupt the waiting state of {@link #waitForEvents(long, TimeUnit)} before completion.
     */
    void cancelListening();
}
