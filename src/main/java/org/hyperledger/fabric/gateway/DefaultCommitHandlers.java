/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway;

import org.hyperledger.fabric.gateway.impl.AllCommitStrategy;
import org.hyperledger.fabric.gateway.impl.AnyCommitStrategy;
import org.hyperledger.fabric.gateway.impl.CommitHandlerImpl;
import org.hyperledger.fabric.gateway.impl.CommitStrategy;
import org.hyperledger.fabric.gateway.impl.NoOpCommitHandler;
import org.hyperledger.fabric.gateway.spi.CommitHandler;
import org.hyperledger.fabric.gateway.spi.CommitHandlerFactory;
import org.hyperledger.fabric.sdk.Peer;

import java.util.Collection;

/**
 * Default commit handler implementations. Instances can be referenced directly or looked up by name, for example
 * {@code DefaultCommitHandlers.valueOf("NONE")}.
 */
public enum DefaultCommitHandlers implements CommitHandlerFactory {
    NONE((transactionId, network) -> NoOpCommitHandler.INSTANCE),

    NETWORK_SCOPE_ALLFORTX((transactionId, network) -> {
        Collection<Peer> peers = network.getChannel().getPeers();
        CommitStrategy strategy = new AllCommitStrategy(peers);
        CommitHandler handler = new CommitHandlerImpl(transactionId, network, strategy);
        return handler;
    }),

    NETWORK_SCOPE_ANYFORTX((transactionId, network) -> {
        Collection<Peer> peers = network.getChannel().getPeers();
        CommitStrategy strategy = new AnyCommitStrategy(peers);
        CommitHandler handler = new CommitHandlerImpl(transactionId, network, strategy);
        return handler;
    });

    private final CommitHandlerFactory factory;

    DefaultCommitHandlers(CommitHandlerFactory factory) {
        this.factory = factory;
    }

    public CommitHandler create(String transactionId, Network network) {
        return factory.create(transactionId, network);
    }

}
