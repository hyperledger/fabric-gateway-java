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

	public ContractException(String message) {
        super(message);
        this.payload = null;
	}

    public ContractException(String message, Throwable cause) {
        super(message, cause);
        this.payload = null;
    }

    public ContractException(String message, byte[] payload) {
		super(message);
		this.payload = payload;
	}

	public Optional<byte[]> getPayload() {
		return Optional.ofNullable(payload);
	}
}
