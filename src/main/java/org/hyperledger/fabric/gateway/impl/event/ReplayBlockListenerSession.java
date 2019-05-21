/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.event;

import org.hyperledger.fabric.gateway.GatewayException;
import org.hyperledger.fabric.gateway.impl.GatewayImpl;
import org.hyperledger.fabric.gateway.impl.NetworkImpl;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;

import java.util.Collection;
import java.util.EnumSet;
import java.util.function.Function;

/**
 * Maintains an isolated client connection for event replay using a listener created by a supplied factory function.
 */
public final class ReplayBlockListenerSession implements ListenerSession {
    private final GatewayImpl gateway;
    private final Channel channel;
    private final BlockEventSource blockSource;
    private final ListenerSession replaySession;

    public ReplayBlockListenerSession(NetworkImpl network, Function<BlockEventSource, ListenerSession> listenerSessionFactory, long startBlock) throws GatewayException {
        this.gateway = network.getGateway().newInstance();
        String channelName = network.getChannel().getName();
        this.channel = gateway.newInstance().getNetwork(channelName).getChannel();

        // Remove old peers first to avoid receiving spurious events from them
        Collection<Peer> eventingPeers = channel.getPeers(EnumSet.of(Peer.PeerRole.EVENT_SOURCE));
        removeAllPeers();

        // Attach listener before replay peers to ensure no replay events are missed
        BlockEventSource channelBlockSource = BlockEventSourceFactory.getInstance().newBlockEventSource(channel);
        this.blockSource =  new OrderedBlockEventSource(channelBlockSource);
        this.replaySession = listenerSessionFactory.apply(blockSource);

        addReplayPeers(eventingPeers, startBlock);
    }

    private void removeAllPeers() throws GatewayException {
        try {
            for (Peer peer : channel.getPeers()) {
                channel.removePeer(peer);
            }
        } catch (InvalidArgumentException e) {
            throw new GatewayException(e);
        }
    }

    private void addReplayPeers(Collection<Peer> eventingPeers, long startBlock) throws GatewayException {
        HFClient client = gateway.getClient();
        try {
            for (Peer originalPeer : eventingPeers) {
                Peer replayPeer = client.newPeer(originalPeer.getName(), originalPeer.getUrl(), originalPeer.getProperties());
                Channel.PeerOptions options = Channel.PeerOptions.createPeerOptions()
                        .addPeerRole(Peer.PeerRole.EVENT_SOURCE)
                        .startEvents(startBlock);
                channel.addPeer(replayPeer, options);
            }
        } catch (InvalidArgumentException e) {
            throw new GatewayException(e);
        }
    }
    @Override
    public void close() {
        replaySession.close();
        blockSource.close();
        gateway.close();
    }
}
