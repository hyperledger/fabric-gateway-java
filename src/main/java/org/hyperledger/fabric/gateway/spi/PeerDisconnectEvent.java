/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.spi;

import org.hyperledger.fabric.sdk.Peer;

/**
 * Event with information relating to a peer disconnect.
 */
public interface PeerDisconnectEvent {
    /**
     * Get the peer that has disconnected.
     * @return A peer.
     */
    Peer getPeer();

    /**
     * Get any underlying exception describing the reason the peer disconnected.
     * @return An exception.
     */
    Throwable getCause();
}
