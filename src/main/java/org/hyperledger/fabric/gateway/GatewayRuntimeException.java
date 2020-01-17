/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway;

/**
 * Runtime exception for gateway classes. Typically indicates misconfiguration or misuse of the API.
 */
public class GatewayRuntimeException extends RuntimeException {
    private static final long serialVersionUID = -1051523683665686266L;

    /**
     * Constructs a new exception with the specified detail message. The cause is not initialized.
     * @param message the detail message.
     */
    public GatewayRuntimeException(final String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified cause. The detail message is not initialized.
     * @param cause the cause.
     */
    public GatewayRuntimeException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new exception with the specified detail message and cause. The payload is not initialized.
     * @param message the detail message.
     * @param cause the cause.
     */
    public GatewayRuntimeException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
