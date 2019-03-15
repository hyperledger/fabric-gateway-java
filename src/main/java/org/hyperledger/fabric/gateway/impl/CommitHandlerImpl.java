/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import org.hyperledger.fabric.gateway.GatewayException;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.impl.event.CompositePeerDisconnectListener;
import org.hyperledger.fabric.gateway.impl.event.PeerDisconnectEvent;
import org.hyperledger.fabric.gateway.impl.event.PeerDisconnectEventSource;
import org.hyperledger.fabric.gateway.impl.event.PeerDisconnectEventSourceFactory;
import org.hyperledger.fabric.gateway.impl.event.TransactionListener;
import org.hyperledger.fabric.gateway.spi.CommitHandler;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.Peer;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public final class CommitHandlerImpl implements CommitHandler {
    private final String transactionId;
    private final Network network;
    private final CommitStrategy strategy;
    private final TransactionListener txListener = this::onTxEvent;
    private final AtomicReference<CompositePeerDisconnectListener> disconnectListener = new AtomicReference<>();
    private final Set<Peer> peers;
    private final CountDownLatch latch = new CountDownLatch(1);
    private final AtomicReference<GatewayException> error = new AtomicReference<>();
    private final long timeoutSeconds = 300; // TODO: Get timeout from gateway options

    public CommitHandlerImpl(String transactionId, Network network, CommitStrategy strategy) {
        this.transactionId = transactionId;
        this.network = network;
        this.strategy = strategy;
        this.peers = Collections.synchronizedSet(new HashSet<>(strategy.getPeers()));
    }

    @Override
    public void startListening() {
        network.getTransactionEventSource().addTransactionListener(txListener);

        PeerDisconnectEventSourceFactory disconnectSourceFactory = PeerDisconnectEventSourceFactory.getInstance();
        Collection<PeerDisconnectEventSource> disconnectEventSources = peers.stream()
                .map(disconnectSourceFactory::getPeerDisconnectEventSource)
                .collect(Collectors.toList());
        disconnectListener.set(new CompositePeerDisconnectListener(this::onDisconnectEvent, disconnectEventSources));
    }

    @Override
    public void waitForEvents() throws GatewayException {
        try {
            latch.await(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new GatewayException(e);
        } finally {
            cancelListening();
        }

        GatewayException cause = error.get();
        if (cause != null) {
            throw cause;
        }
    }

    @Override
    public synchronized void cancelListening() {
        latch.countDown();
        network.getTransactionEventSource().removeTransactionListener(txListener);
        disconnectListener.get().close();
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
            fail(new GatewayException("Transaction commit was rejected by peer " + peerName));
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
            fail(new GatewayException("Commit strategy failed"));
        }
    }

    private void fail(GatewayException e) {
        error.set(e);
        cancelListening();
    }
}
