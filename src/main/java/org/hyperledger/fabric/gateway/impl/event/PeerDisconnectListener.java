/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.event;

/**
 * Functional interface for listening to peer disconnect events.
 */
@FunctionalInterface
public interface PeerDisconnectListener {
    void peerDisconnected(PeerDisconnectEvent peer);
}
