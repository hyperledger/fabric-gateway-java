/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway;

import java.util.Optional;

/**
 * Thrown when an error occurs invoking a smart contract.
 */
public class ContractException extends GatewayException {
    private final byte[] payload;

    /**
     * Constructs a new exception with the specified detail message. The cause and payload are not initialized.
     * @param message the detail message.
     */
    public ContractException(final String message) {
        super(message);
        this.payload = null;
    }

    /**
     * Constructs a new exception with the specified detail message and cause. The payload is not initialized.
     * @param message the detail message.
     * @param cause the cause.
     */
    public ContractException(final String message, final Throwable cause) {
        super(message, cause);
        this.payload = null;
    }

    /**
     * Constructs a new exception with the specified detail message and response payload. The cause is not initialized.
     * @param message the detail message.
     * @param payload the error response payload.
     */
    public ContractException(final String message, final byte[] payload) {
        super(message);
        this.payload = payload;
    }

    /**
     * Get the error response payload received from the smart contract.
     * @return the response payload.
     */
    public Optional<byte[]> getPayload() {
        return Optional.ofNullable(payload);
    }
}
