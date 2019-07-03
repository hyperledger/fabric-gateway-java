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
	public GatewayRuntimeException(String message) {
		super(message);
	}

	public GatewayRuntimeException(Throwable cause) {
		super(cause);
	}

	public GatewayRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}
}
