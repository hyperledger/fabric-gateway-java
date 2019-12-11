/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.commit;

import java.util.concurrent.TimeUnit;

import org.hyperledger.fabric.gateway.spi.CommitHandler;

public enum NoOpCommitHandler implements CommitHandler {
    INSTANCE;

    @Override
    public void startListening() { }

    @Override
    public void waitForEvents(final long timeout, final TimeUnit timeUnit) { }

    @Override
    public void cancelListening() { }
}
