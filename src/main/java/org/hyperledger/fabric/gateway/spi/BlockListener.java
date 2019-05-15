/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.spi;

import org.hyperledger.fabric.sdk.BlockEvent;

/**
 * Functional interface for block listening.
 */
@FunctionalInterface
public interface BlockListener {
    void receivedBlock(BlockEvent blockEvent);
}
