/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.spi;

import org.hyperledger.fabric.gateway.GatewayException;

import java.util.concurrent.TimeoutException;

public interface CommitHandler {
    /**
     * Called to initiate listening for transaction events.
     */
    void startListening();

    /**
     * Wait until enough transaction commit events have been received to satisfy the event handling strategy.
     * @throws GatewayException
     * @throws TimeoutException
     */
    void waitForEvents() throws GatewayException, TimeoutException;

    /**
     * Called to interrupt the waiting state of {@link #waitForEvents()} before completion.
     */
    void cancelListening();
}
