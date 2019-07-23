/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.sample;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.spi.CommitHandler;
import org.hyperledger.fabric.gateway.spi.CommitListener;
import org.hyperledger.fabric.gateway.spi.PeerDisconnectEvent;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.Peer;

/**
 * Commit handler implementation that unblocks once all the peers it is observing either commit the transaction or
 * disconnect, regardless of how many commits are observed. Note that the implementation is thread-safe.
 */
public final class SampleCommitHandler implements CommitHandler {
    private final Network network;
    private final Set<Peer> peers;
    private final String transactionId;
    private final CountDownLatch completeLatch = new CountDownLatch(1);
    private final CommitListener listener = new CommitListener() {
        @Override
        public void acceptCommit(BlockEvent.TransactionEvent transactionEvent) {
            acceptEvent(transactionEvent.getPeer());
        }

        @Override
        public void acceptDisconnect(PeerDisconnectEvent disconnectEvent) {
            acceptEvent(disconnectEvent.getPeer());
        }
    };

    public SampleCommitHandler(Network network, Collection<Peer> peers, String transactionId) {
        this.network = network;
        this.peers = Collections.synchronizedSet(new HashSet<>(peers));
        this.transactionId = transactionId;

        if (this.peers.isEmpty()) {
            throw new IllegalArgumentException("No peers specified");
        }
    }

    @Override
    public void startListening() {
        network.addCommitListener(listener, peers, transactionId);
    }

    @Override
    public void waitForEvents(long timeout, TimeUnit timeUnit) throws TimeoutException, InterruptedException {
        try {
            boolean complete = completeLatch.await(timeout, timeUnit);
            if (!complete) {
                throw new TimeoutException("Timeout waiting for commit of transaction " + transactionId);
            }
        } finally {
            cancelListening();
        }
    }

    @Override
    public void cancelListening() {
        peers.clear();
        network.removeCommitListener(listener);
        completeLatch.countDown();
    }

    private void acceptEvent(Peer peer) {
        if (peers.remove(peer) && peers.isEmpty()) {
            cancelListening();
        }
    }
}
