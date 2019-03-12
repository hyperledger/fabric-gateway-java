/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway;

public interface Network {
  Contract getContract(String chaincodeId);

  Contract getContract(String chaincodeId, String name);

  Gateway getGateway();
}