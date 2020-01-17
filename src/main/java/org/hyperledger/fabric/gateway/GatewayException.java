/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway;

/**
 * Base class for exceptions thrown by the Gateway SDK or by components of the underlying Fabric.
 */
public class GatewayException extends Exception {
    private static final long serialVersionUID = -6313392932347237084L;

    /**
     * Constructs a new exception with the specified detail message. The cause is not initialized.
     * @param message the detail message.
     */
    public GatewayException(final String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified cause. The detail message is not initialized.
     * @param cause the cause.
     */
    public GatewayException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     * @param message the detail message.
     * @param cause the cause.
     */
    public GatewayException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
