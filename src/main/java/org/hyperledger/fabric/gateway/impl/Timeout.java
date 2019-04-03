/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import java.util.concurrent.TimeUnit;

/**
 * Encapsulates the value and time unit for timeouts typically required on blocking method calls in the Java
 * concurrency libraries.
 */
public class Timeout {
    private final long timeout;
    private final TimeUnit timeUnit;

    public Timeout(long timeout, TimeUnit timeUnit) {
        this.timeout = timeout;
        this.timeUnit = timeUnit;
    }

    public long getTimeout() {
        return timeout;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    @Override
    public String toString() {
        return String.format("%s %s", timeout, timeUnit);
    }
}
