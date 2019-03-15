/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.event;

/**
 * Allows observing received block events.
 */
public interface BlockEventSource extends AutoCloseable {
    BlockListener addBlockListener(BlockListener listener);
    void removeBlockListener(BlockListener listener);

    @Override
    void close();
}
