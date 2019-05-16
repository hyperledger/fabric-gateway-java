/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.event;

import org.hyperledger.fabric.sdk.BlockEvent;

import java.util.function.Consumer;

/**
 * Allows observing received block events.
 */
public interface BlockEventSource extends AutoCloseable {
    Consumer<BlockEvent> addBlockListener(Consumer<BlockEvent> listener);
    void removeBlockListener(Consumer<BlockEvent> listener);

    @Override
    void close();
}
