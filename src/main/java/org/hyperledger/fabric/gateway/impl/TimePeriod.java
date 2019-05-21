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
public final class TimePeriod {
    private final long time;
    private final TimeUnit timeUnit;

    public TimePeriod(long time, TimeUnit timeUnit) {
        this.time = time;
        this.timeUnit = timeUnit;
    }

    public long getTime() {
        return time;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    @Override
    public String toString() {
        return String.format("%s %s", time, timeUnit);
    }
}
