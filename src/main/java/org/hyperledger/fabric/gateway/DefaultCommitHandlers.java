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
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;

import java.util.Collection;

/**
 * Default commit handler implementations. Instances can be referenced directly or looked up by name, for example
 * {@code DefaultCommitHandlers.valueOf("NONE")}.
 */
public enum DefaultCommitHandlers implements CommitHandlerFactory {
    /**
     * Do not wait for any commit events to be received from peers after submitting a transaction.
     */
    NONE((transactionId, network) -> NoOpCommitHandler.INSTANCE),

    /**
     * Wait to receive commit events from all currently responding peers in the user's organization after submitting
     * a transaction.
     */
    MSPID_SCOPE_ALLFORTX((transactionId, network) -> {
        Collection<Peer> peers = getPeersForOrganization(network);
        CommitStrategy strategy = new AllCommitStrategy(peers);
        CommitHandler handler = new CommitHandlerImpl(transactionId, network, strategy);
        return handler;
    }),

    /**
     * Wait to receive commit events from all currently responding peers in the network after submitting a transaction.
     */
    NETWORK_SCOPE_ALLFORTX((transactionId, network) -> {
        Collection<Peer> peers = network.getChannel().getPeers();
        CommitStrategy strategy = new AllCommitStrategy(peers);
        CommitHandler handler = new CommitHandlerImpl(transactionId, network, strategy);
        return handler;
    }),

    /**
     * Wait to receive a commit event from any currently responding peer in the user's organization after submitting
     * a transaction.
     */
    MSPID_SCOPE_ANYFORTX((transactionId, network) -> {
        Collection<Peer> peers = getPeersForOrganization(network);
        CommitStrategy strategy = new AnyCommitStrategy(peers);
        CommitHandler handler = new CommitHandlerImpl(transactionId, network, strategy);
        return handler;
    }),

    /**
     * Wait to receive a commit event from any currently responding peer in the network after submitting a transaction.
     */
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

    private static Collection<Peer> getPeersForOrganization(Network network) {
        String mspId = network.getGateway().getIdentity().getMspId();
        try {
            return network.getChannel().getPeersForOrganization(mspId);
        } catch (InvalidArgumentException e) {
            // This should never happen as mspId should not be null
            throw new RuntimeException(e);
        }
    }

    public CommitHandler create(String transactionId, Network network) {
        return factory.create(transactionId, network);
    }
}
