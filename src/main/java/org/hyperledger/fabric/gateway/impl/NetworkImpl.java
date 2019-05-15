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
import org.hyperledger.fabric.gateway.impl.event.OrderedBlockEventSource;
import org.hyperledger.fabric.gateway.impl.event.TransactionEventSource;
import org.hyperledger.fabric.gateway.impl.event.TransactionEventSourceImpl;
import org.hyperledger.fabric.gateway.spi.BlockListener;
import org.hyperledger.fabric.gateway.spi.QueryHandler;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.TransactionException;

import java.util.HashMap;
import java.util.Map;

public class NetworkImpl implements Network {
    private final Channel channel;
    private final GatewayImpl gateway;
    private final Map<String, Contract> contracts = new HashMap<>();
    private final BlockEventSource blockSource;
    private final TransactionEventSource transactionSource;
    private final QueryHandler queryHandler;
    private final PeerTracker peerTracker;

    NetworkImpl(Channel channel, GatewayImpl gateway) throws GatewayException {
        this.channel = channel;
        this.gateway = gateway;
        this.peerTracker = new PeerTracker(channel);
        gateway.getNetworkConfig().ifPresent(peerTracker::loadNetworkConfig);

        initializeChannel();

        BlockEventSource rawBlockSource = BlockEventSourceFactory.getInstance().newBlockEventSource(channel);
        blockSource = new OrderedBlockEventSource(rawBlockSource);
        transactionSource = new TransactionEventSourceImpl(rawBlockSource);
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
    public synchronized Contract getContract(String chaincodeId, String name) {
        if (chaincodeId == null || chaincodeId.isEmpty()) {
            throw new IllegalArgumentException("getContract: chaincodeId must be a non-empty string");
        }
        if (name == null) {
            throw new IllegalArgumentException("getContract: name must not be null");
        }

        String key = chaincodeId + ':' + name;
        Contract contract = contracts.get(key);
        if (contract == null) {
            contract = new ContractImpl(this, chaincodeId, name);
            contracts.put(key, contract);
        }
        return contract;
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

    public QueryHandler getQueryHandler() {
        return queryHandler;
    }

    @Override
    public String getPeerOrganization(Peer peer) {
        String mspId = peerTracker.getPeerOrganization(peer);
        if (mspId == null) {
            throw new IllegalArgumentException("Peer is not a network member: " + peer);
        }
        return mspId;
    }

    @Override
    public BlockListener addBlockListener(BlockListener listener) {
        return blockSource.addBlockListener(listener);
    }

    @Override
    public void removeBlockListener(BlockListener listener) {
        blockSource.removeBlockListener(listener);
    }
}
