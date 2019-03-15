/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.spi;

import org.hyperledger.fabric.gateway.Network;

/**
 * Functional interface describing a factory method for constructing commit handler instances.
 */
public interface CommitHandlerFactory {
    CommitHandler create(String transactionId, Network network);
}
