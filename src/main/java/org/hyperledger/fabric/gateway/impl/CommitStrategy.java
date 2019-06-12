/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import org.hyperledger.fabric.gateway.spi.PeerDisconnectEvent;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.Peer;

import java.util.Collection;

public interface CommitStrategy {
    enum Result {
        SUCCESS, FAIL, CONTINUE
    }

    Collection<Peer> getPeers();
    Result onEvent(BlockEvent.TransactionEvent event);
    Result onError(PeerDisconnectEvent event);
}
