/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.event;

/**
 * Allows observing peer disconnect events.
 */
public interface PeerDisconnectEventSource extends AutoCloseable {
    PeerDisconnectListener addDisconnectListener(PeerDisconnectListener listener);
    void removeDisconnectListener(PeerDisconnectListener listener);

    @Override
    void close();
}
