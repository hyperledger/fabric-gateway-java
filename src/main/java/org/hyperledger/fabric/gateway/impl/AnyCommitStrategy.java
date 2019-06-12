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

public final class AnyCommitStrategy implements CommitStrategy {
    private final Collection<Peer> peers;
    private int totalCount = 0;

    public AnyCommitStrategy(Collection<Peer> peers) {
        this.peers = peers;
    }

    @Override
    public Collection<Peer> getPeers() {
        return peers;
    }

    @Override
    public synchronized Result onEvent(BlockEvent.TransactionEvent event) {
        return Result.SUCCESS;
    }

    @Override
    public synchronized Result onError(PeerDisconnectEvent event) {
        totalCount++;
        if (totalCount < peers.size()) {
            return Result.CONTINUE;
        } else {
            return Result.FAIL;
        }
    }
}
