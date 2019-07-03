/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.hyperledger.fabric.gateway.ContractException;
import org.hyperledger.fabric.gateway.GatewayRuntimeException;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.spi.CommitHandler;
import org.hyperledger.fabric.gateway.spi.CommitListener;
import org.hyperledger.fabric.gateway.spi.PeerDisconnectEvent;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.Peer;

public final class CommitHandlerImpl implements CommitHandler {
    private final String transactionId;
    private final Network network;
    private final CommitStrategy strategy;
    private final CommitListener listener = new CommitListener() {
        @Override
        public void acceptCommit(BlockEvent.TransactionEvent transactionEvent) {
            onTxEvent(transactionEvent);
        }

        @Override
        public void acceptDisconnect(PeerDisconnectEvent disconnectEvent) {
            onDisconnectEvent(disconnectEvent);
        }
    };
    private final Set<Peer> peers;
    private final CountDownLatch latch = new CountDownLatch(1);
    private final AtomicReference<ContractException> error = new AtomicReference<>();

    public CommitHandlerImpl(String transactionId, Network network, CommitStrategy strategy) {
        this.transactionId = transactionId;
        this.network = network;
        this.strategy = strategy;
        this.peers = Collections.synchronizedSet(new HashSet<>(strategy.getPeers()));
    }

    @Override
    public void startListening() {
        network.addCommitListener(listener, peers, transactionId);
    }

    @Override
    public void waitForEvents(long timeout, TimeUnit timeUnit) throws ContractException {
        try {
            latch.await(timeout, timeUnit);
        } catch (InterruptedException e) {
            throw new GatewayRuntimeException(e);
        } finally {
            cancelListening();
        }

        ContractException cause = error.get();
        if (cause != null) {
            throw cause;
        }
    }

    @Override
    public synchronized void cancelListening() {
        latch.countDown();
        network.removeCommitListener(listener);
        peers.clear();
    }

    @Override
    public String toString() {
        return String.format("%s(transactionId=%s, strategy=%s, peers=%s)", getClass().getSimpleName(), transactionId, strategy, peers);
    }

    private void onTxEvent(BlockEvent.TransactionEvent event) {
        if (!transactionId.equals(event.getTransactionID()) || !peers.remove(event.getPeer())) {
            // Not a transaction or peer we were looking for
            return;
        }

        if (event.isValid()) {
            CommitStrategy.Result result = strategy.onEvent(event);
            processStrategyResult(result);
        } else {
            String peerName = event.getPeer().getName();
            fail(new ContractException("Transaction commit was rejected by peer " + peerName));
        }
    }

    private void onDisconnectEvent(PeerDisconnectEvent event) {
        if (!peers.remove(event.getPeer())) {
            // Not a peer we were looking for
            return;
        }

        CommitStrategy.Result result = strategy.onError(event);
        processStrategyResult(result);
    }

    private void processStrategyResult(CommitStrategy.Result strategyResult) {
        if (strategyResult == CommitStrategy.Result.SUCCESS) {
            cancelListening();
        } else if (strategyResult == CommitStrategy.Result.FAIL) {
            fail(new ContractException("Commit strategy failed"));
        }
    }

    private void fail(ContractException e) {
        error.set(e);
        cancelListening();
    }
}
