/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.GatewayRuntimeException;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.impl.event.BlockEventSource;
import org.hyperledger.fabric.gateway.impl.event.BlockEventSourceFactory;
import org.hyperledger.fabric.gateway.impl.event.BlockListenerSession;
import org.hyperledger.fabric.gateway.impl.event.CommitListenerSession;
import org.hyperledger.fabric.gateway.impl.event.ListenerSession;
import org.hyperledger.fabric.gateway.impl.event.Listeners;
import org.hyperledger.fabric.gateway.impl.event.OrderedBlockEventSource;
import org.hyperledger.fabric.gateway.impl.event.ReplayListenerSession;
import org.hyperledger.fabric.gateway.spi.Checkpointer;
import org.hyperledger.fabric.gateway.spi.CommitListener;
import org.hyperledger.fabric.gateway.spi.QueryHandler;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.TransactionException;

public final class NetworkImpl implements Network, AutoCloseable {
    private final Channel channel;
    private final GatewayImpl gateway;
    private final Map<String, Contract> contracts = new ConcurrentHashMap<>();
    private final BlockEventSource channelBlockSource;
    private final BlockEventSource orderedBlockSource;
    private final QueryHandler queryHandler;
    private final Map<Consumer<BlockEvent>, ListenerSession> blockListenerSessions = new HashMap<>();
    private final Map<CommitListener, CommitListenerSession> commitListenerSessions = new ConcurrentHashMap<>();

    NetworkImpl(Channel channel, GatewayImpl gateway) {
        this.channel = channel;
        this.gateway = gateway;

        initializeChannel();

        channelBlockSource = BlockEventSourceFactory.getInstance().newBlockEventSource(channel);
        orderedBlockSource = new OrderedBlockEventSource(channelBlockSource);
        queryHandler = gateway.getQueryHandlerFactory().create(this);
    }

    private void initializeChannel() {
        try {
            channel.initialize();
        } catch (InvalidArgumentException | TransactionException e) {
            throw new GatewayRuntimeException("Failed to initialize channel", e);
        }
    }

    @Override
    public Contract getContract(final String chaincodeId, final String name) {
        if (chaincodeId == null || chaincodeId.isEmpty()) {
            throw new IllegalArgumentException("getContract: chaincodeId must be a non-empty string");
        }
        if (name == null) {
            throw new IllegalArgumentException("getContract: name must not be null");
        }

        String key = chaincodeId + ':' + name;
        return contracts.computeIfAbsent(key, k -> new ContractImpl(this, chaincodeId, name));
    }

    @Override
    public Contract getContract(String chaincodeId) {
        return getContract(chaincodeId, "");
    }

    @Override
    public GatewayImpl getGateway() {
        return gateway;
    }

    @Override
    public Channel getChannel() {
        return channel;
    }

    @Override
    public Consumer<BlockEvent> addBlockListener(Consumer<BlockEvent> listener) {
        synchronized (blockListenerSessions) {
            blockListenerSessions.computeIfAbsent(listener, k -> new BlockListenerSession(orderedBlockSource, listener));
        }
        return listener;
    }

    @Override
    public Consumer<BlockEvent> addBlockListener(Checkpointer checkpointer, Consumer<BlockEvent> listener) throws IOException {
        synchronized (blockListenerSessions) {
            if (!blockListenerSessions.containsKey(listener)) {
                Consumer<BlockEvent> checkpointListener = Listeners.checkpointBlock(checkpointer, listener);
                ListenerSession session = newCheckpointListenerSession(checkpointer, checkpointListener);
                blockListenerSessions.put(listener, session);
            }
        }

        return listener;
    }

    @Override
    public Consumer<BlockEvent> addBlockListener(long startBlock, Consumer<BlockEvent> listener) {
        synchronized (blockListenerSessions) {
            if (!blockListenerSessions.containsKey(listener)) {
                ListenerSession session = new ReplayListenerSession(this, listener, startBlock);
                blockListenerSessions.put(listener, session);
            }
        }
        return listener;
    }

    public ListenerSession newCheckpointListenerSession(Checkpointer checkpointer, Consumer<BlockEvent> listener) throws IOException {
        final long blockNumber = checkpointer.getBlockNumber();
        if (blockNumber == Checkpointer.UNSET_BLOCK_NUMBER) {
            // New checkpointer so can attach to the shared block source
            return new BlockListenerSession(orderedBlockSource, listener);
        }
        return new ReplayListenerSession(this, listener, blockNumber);
    }

    @Override
    public void removeBlockListener(Consumer<BlockEvent> listener) {
        final ListenerSession session;
        synchronized (blockListenerSessions) {
            session = blockListenerSessions.remove(listener);
        }
        if (session != null) {
            session.close();
        }
    }

    @Override
    public CommitListener addCommitListener(CommitListener listener, Collection<Peer> peers, String transactionId) {
        commitListenerSessions.computeIfAbsent(listener, k ->
                new CommitListenerSession(channelBlockSource, listener, peers, transactionId));
        return listener;
    }

    @Override
    public void removeCommitListener(CommitListener listener) {
        CommitListenerSession session = commitListenerSessions.remove(listener);
        if (session != null) {
            session.close();
        }
    }

    public QueryHandler getQueryHandler() {
        return queryHandler;
    }

    public BlockEventSource getBlockSource() {
        return orderedBlockSource;
    }

    @Override
    public void close() {
        synchronized (blockListenerSessions) {
            blockListenerSessions.values().forEach(ListenerSession::close);
            blockListenerSessions.clear();
        }
        commitListenerSessions.values().forEach(ListenerSession::close);
        commitListenerSessions.clear();

        orderedBlockSource.close();
        channelBlockSource.close();

        channel.shutdown(false);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '@' + System.identityHashCode(this) +
                "(name=" + channel.getName() +
                ", channelBlockSource=" + channelBlockSource +
                ", commitListenerSessions=" + commitListenerSessions +
                ", orderedBlockSource=" + orderedBlockSource +
                ", blockListenerSessions=" + blockListenerSessions + ')';
    }
}
