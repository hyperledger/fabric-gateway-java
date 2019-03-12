/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway;

import java.util.concurrent.TimeoutException;

public interface Contract {
  Transaction createTransaction(String name);

  byte[] submitTransaction(String name, String... args) throws GatewayException, TimeoutException;

  byte[] evaluateTransaction(String name, String... args) throws GatewayException;
}
