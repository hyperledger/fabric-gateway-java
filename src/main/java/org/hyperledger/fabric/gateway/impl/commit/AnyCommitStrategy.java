/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.commit;

import java.util.Collection;

import org.hyperledger.fabric.gateway.spi.PeerDisconnectEvent;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.Peer;

public final class AnyCommitStrategy implements CommitStrategy {
    private final Collection<Peer> peers;
    private int totalCount = 0;

    public AnyCommitStrategy(final Collection<Peer> peers) {
        this.peers = peers;
    }

    @Override
    public Collection<Peer> getPeers() {
        return peers;
    }

    @Override
    public synchronized Result onEvent(final BlockEvent.TransactionEvent event) {
        return Result.SUCCESS;
    }

    @Override
    public synchronized Result onError(final PeerDisconnectEvent event) {
        totalCount++;
        if (totalCount < peers.size()) {
            return Result.CONTINUE;
        } else {
            return Result.FAIL;
        }
    }
}
