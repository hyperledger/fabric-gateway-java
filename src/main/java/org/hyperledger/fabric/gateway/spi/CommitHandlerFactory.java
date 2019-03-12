/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.spi;

import org.hyperledger.fabric.network.Network;

public interface CommitHandlerFactory {
  CommitHandler create(String transactionId, Network network);
}
