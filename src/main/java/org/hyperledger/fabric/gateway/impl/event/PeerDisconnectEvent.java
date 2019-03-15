/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.event;

import org.hyperledger.fabric.sdk.Peer;

/**
 * Event with information relating to a peer disconnect.
 */
public interface PeerDisconnectEvent {
    Peer getPeer();
    Throwable getCause();
}
