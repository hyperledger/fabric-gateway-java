/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.sample;

import java.util.Collection;
import java.util.EnumSet;

import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.spi.CommitHandler;
import org.hyperledger.fabric.gateway.spi.CommitHandlerFactory;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;

/**
 * Commit handler factory implementation that creates sample commit handler instances configured with all the eventing
 * peers from the current user's organization. The singleton instance is accessed as
 * {@code SampleCommitHandlerFactory.INSTANCE}, and can be configured on a Gateway with:
 * <pre>
 * Gateway.Builder builder = Gateway.createBuilder()
 *         .commitHandler(SampleCommitHandlerFactory.INSTANCE);
 * </pre>
 */
public enum SampleCommitHandlerFactory implements CommitHandlerFactory {
    INSTANCE;

    private static final EnumSet<Peer.PeerRole> EVENT_SOURCE_ROLES = EnumSet.of(Peer.PeerRole.EVENT_SOURCE);

    private static Collection<Peer> getEventSourcePeersForOrganization(Network network) {
        Collection<Peer> eventSourcePeers = getEventSourcePeers(network);
        Collection<Peer> orgPeers = getPeersForOrganization(network);
        orgPeers.retainAll(eventSourcePeers);
        return orgPeers;
    }
    private static Collection<Peer> getEventSourcePeers(Network network) {
        return network.getChannel().getPeers(EVENT_SOURCE_ROLES);
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

    @Override
    public CommitHandler create(String transactionId, Network network) {
        Collection<Peer> orgPeers = getEventSourcePeersForOrganization(network);
        return new SampleCommitHandler(network, orgPeers, transactionId);
    }
}
