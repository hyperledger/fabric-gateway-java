/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */


package org.hyperledger.fabric.gateway;

import java.util.Map;
import java.util.concurrent.TimeoutException;

public interface Transaction {
  String getName();

  String getTransactionId();

  void setTransient(Map<String, byte[]> transientData);

  byte[] submit(String... args) throws GatewayException, TimeoutException;

  byte[] evaluate(String... args) throws GatewayException;
}
