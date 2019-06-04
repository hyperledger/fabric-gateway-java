/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.GatewayException;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.impl.event.BlockEventSource;
import org.hyperledger.fabric.gateway.impl.event.BlockEventSourceFactory;
import org.hyperledger.fabric.gateway.impl.event.CheckpointBlockListenerSession;
import org.hyperledger.fabric.gateway.impl.event.ContractEventSource;
import org.hyperledger.fabric.gateway.impl.event.ContractEventSourceFactory;
import org.hyperledger.fabric.gateway.impl.event.ListenerSession;
import org.hyperledger.fabric.gateway.impl.event.OrderedBlockEventSource;
import org.hyperledger.fabric.gateway.impl.event.ReplayBlockListenerSession;
import org.hyperledger.fabric.gateway.impl.event.SimpleBlockListenerSession;
import org.hyperledger.fabric.gateway.impl.event.TransactionEventSource;
import org.hyperledger.fabric.gateway.impl.event.TransactionEventSourceImpl;
import org.hyperledger.fabric.gateway.spi.Checkpointer;
import org.hyperledger.fabric.gateway.spi.QueryHandler;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.TransactionException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class NetworkImpl implements Network {
    private final Channel channel;
    private final GatewayImpl gateway;
    private final Map<String, Contract> contracts = new ConcurrentHashMap<>();
    private final BlockEventSource blockSource;
    private final TransactionEventSource transactionSource;
    private final QueryHandler queryHandler;
    private final ContractEventSource contractEventSource;
    private final Map<Consumer<BlockEvent>, ListenerSession> blockListenerSessions = new HashMap<>();

    NetworkImpl(Channel channel, GatewayImpl gateway) throws GatewayException {
        this.channel = channel;
        this.gateway = gateway;

        initializeChannel();

        BlockEventSource channelBlockSource = BlockEventSourceFactory.getInstance().newBlockEventSource(channel);
        blockSource = new OrderedBlockEventSource(channelBlockSource);
        transactionSource = new TransactionEventSourceImpl(channelBlockSource);
        contractEventSource = ContractEventSourceFactory.getInstance().newContractEventSource(channel);
        queryHandler = gateway.getQueryHandlerFactory().create(this);
    }

    private void initializeChannel() throws GatewayException {
        try {
            channel.initialize();
        } catch (InvalidArgumentException | TransactionException e) {
            throw new GatewayException(e);
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
    public TransactionEventSource getTransactionEventSource() {
        return transactionSource;
    }

    @Override
    public Consumer<BlockEvent> addBlockListener(Consumer<BlockEvent> listener) {
        synchronized (blockListenerSessions) {
            blockListenerSessions.computeIfAbsent(listener, k -> new SimpleBlockListenerSession(blockSource, listener));
        }
        return listener;
    }

    @Override
    public Consumer<BlockEvent> addBlockListener(Consumer<BlockEvent> listener, Checkpointer checkpointer) throws GatewayException, IOException {
        final long blockNumber = checkpointer.getBlockNumber();

        synchronized (blockListenerSessions) {
            if (!blockListenerSessions.containsKey(listener)) {
                final ListenerSession session;
                if (blockNumber == Checkpointer.UNSET_BLOCK_NUMBER) {
                    // New checkpointer so can attach to the shared block source
                    session = new CheckpointBlockListenerSession(blockSource, listener, checkpointer);
                } else {
                    session = new ReplayBlockListenerSession(this,
                            blockSource -> new CheckpointBlockListenerSession(blockSource, listener, checkpointer),
                            blockNumber);
                }
                blockListenerSessions.put(listener, session);
            }
        }

        return listener;
    }

//    @Override
    public Consumer<BlockEvent> addBlockListener(Consumer<BlockEvent> listener, String checkpointerName) throws GatewayException, IOException {
        Checkpointer checkpointer = gateway.getCheckpointerFactory().create(channel.getName(), checkpointerName);
        return addBlockListener(listener, checkpointer);
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

    public QueryHandler getQueryHandler() {
        return queryHandler;
    }

    public ContractEventSource getContractEventSource() {
        return contractEventSource;
    }
}
