/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.impl.event.BlockEventSource;
import org.hyperledger.fabric.gateway.impl.event.BlockEventSourceFactory;
import org.hyperledger.fabric.gateway.impl.event.TransactionEventSource;
import org.hyperledger.fabric.gateway.impl.event.TransactionEventSourceImpl;
import org.hyperledger.fabric.sdk.Channel;

import java.util.HashMap;
import java.util.Map;

public class NetworkImpl implements Network {
    private final Channel channel;
    private final GatewayImpl gateway;
    private final Map<String, Contract> contracts = new HashMap<>();
    private final BlockEventSource blockSource;
    private final TransactionEventSource transactionSource;

    NetworkImpl(Channel channel, GatewayImpl gateway) {
        this.channel = channel;
        this.gateway = gateway;
        blockSource = BlockEventSourceFactory.getInstance().newBlockEventSource(channel);
        transactionSource = new TransactionEventSourceImpl(blockSource);
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
}
