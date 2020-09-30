/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway;

import java.util.Collection;
import java.util.EnumSet;

import org.hyperledger.fabric.gateway.impl.query.RoundRobinQueryHandler;
import org.hyperledger.fabric.gateway.impl.query.SingleQueryHandler;
import org.hyperledger.fabric.gateway.spi.QueryHandler;
import org.hyperledger.fabric.gateway.spi.QueryHandlerFactory;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;

/**
 * Default query handler implementations. Instances can be referenced directly or looked up by name, for example
 * {@code DefaultQueryHandlers.valueOf("MSPID_SCOPE_ROUND_ROBIN")}.
 */
public enum DefaultQueryHandlers implements QueryHandlerFactory {
    /**
     * The last peer that provided a successful response is used. If a peer fails then all other peers will be tried
     * in turn until one provides a successful response. If no peers respond then an exception is thrown.
     */
    MSPID_SCOPE_SINGLE(network -> {
        Collection<Peer> peers = getChaincodeQueryPeersForOrganization(network);
        return new SingleQueryHandler(peers);
    }),

    /**
     * For each subsequent query, the next peer in the list is used. If a peer fails then all other peers will be tried
     * in turn until one provides a successful response. If no peers respond then an exception is thrown.
     */
    MSPID_SCOPE_ROUND_ROBIN(network -> {
        Collection<Peer> peers = getChaincodeQueryPeersForOrganization(network);
        return new RoundRobinQueryHandler(peers);
    }),

    /**
     * The last peer that provided a successful response is used. If a peer fails then all other peers will be tried
     * in turn until one provides a successful response. If no peers respond then an exception is thrown. If the user's
     * organization has no peers, then all peers in the network will be used.
     */
    PREFER_MSPID_SCOPE_SINGLE(network -> {
        Collection<Peer> peers = getChaincodeQueryPeersForOrganization(network);
        if (peers.isEmpty()) {
            peers = getChaincodeQueryPeers(network);
        }
        return new SingleQueryHandler(peers);
    }),

    /**
     * For each subsequent query, the next peer in the list is used. If a peer fails then all other peers will be tried
     * in turn until one provides a successful response. If no peers respond then an exception is thrown. If the user's
     * organization has no peers, then all peers in the network will be used.
     */
    PREFER_MSPID_SCOPE_ROUND_ROBIN(network -> {
        Collection<Peer> peers = getChaincodeQueryPeersForOrganization(network);
        if (peers.isEmpty()) {
            peers = getChaincodeQueryPeers(network);
        }
        return new RoundRobinQueryHandler(peers);
    });

    private static final EnumSet<Peer.PeerRole> QUERY_ROLES = EnumSet.of(Peer.PeerRole.CHAINCODE_QUERY);

    private final QueryHandlerFactory factory;

    DefaultQueryHandlers(final QueryHandlerFactory factory) {
        this.factory = factory;
    }

    private static Collection<Peer> getChaincodeQueryPeersForOrganization(final Network network) {
        Collection<Peer> queryPeers = getChaincodeQueryPeers(network);
        Collection<Peer> orgPeers = getPeersForOrganization(network);
        orgPeers.retainAll(queryPeers);
        return orgPeers;
    }

    private static Collection<Peer> getChaincodeQueryPeers(final Network network) {
        return network.getChannel().getPeers(QUERY_ROLES);
    }

    private static Collection<Peer> getPeersForOrganization(final Network network) {
        String mspId = network.getGateway().getIdentity().getMspId();
        try {
            return network.getChannel().getPeersForOrganization(mspId);
        } catch (InvalidArgumentException e) {
            // This should never happen as mspId should not be null
            throw new RuntimeException(e);
        }
    }

    @Override
    public QueryHandler create(final Network network) {
        return factory.create(network);
    }
}
