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

public final class AllCommitStrategy implements CommitStrategy {
    private final Collection<Peer> peers;
    private int successCount = 0;
    private int totalCount = 0;

    public AllCommitStrategy(final Collection<Peer> peers) {
        this.peers = peers;
    }

    @Override
    public Collection<Peer> getPeers() {
        return peers;
    }

    @Override
    public synchronized Result onEvent(final BlockEvent.TransactionEvent event) {
        successCount++;
        totalCount++;
        return getResult();
    }

    @Override
    public synchronized Result onError(final PeerDisconnectEvent event) {
        totalCount++;
        return getResult();
    }

    private Result getResult() {
        if (totalCount < peers.size()) {
            return Result.CONTINUE;
        } else if (successCount >= 1) {
            return Result.SUCCESS;
        } else {
            return Result.FAIL;
        }
    }
}
