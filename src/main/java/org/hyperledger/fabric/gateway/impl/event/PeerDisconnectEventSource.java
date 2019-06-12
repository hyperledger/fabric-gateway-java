/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.event;

import org.hyperledger.fabric.gateway.spi.PeerDisconnectEvent;

import java.util.function.Consumer;

/**
 * Allows observing peer disconnect events.
 */
public interface PeerDisconnectEventSource extends AutoCloseable {
    Consumer<PeerDisconnectEvent> addDisconnectListener(Consumer<PeerDisconnectEvent> listener);
    void removeDisconnectListener(Consumer<PeerDisconnectEvent> listener);

    @Override
    void close();
}
