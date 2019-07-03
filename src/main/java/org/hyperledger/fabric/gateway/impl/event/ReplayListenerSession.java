/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.event;

import java.util.Collection;
import java.util.EnumSet;
import java.util.function.Consumer;

import org.hyperledger.fabric.gateway.GatewayRuntimeException;
import org.hyperledger.fabric.gateway.impl.GatewayImpl;
import org.hyperledger.fabric.gateway.impl.NetworkImpl;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;

/**
 * Maintains an isolated client connection for event replay using a listener created by a supplied factory function.
 */
public final class ReplayListenerSession implements ListenerSession {
    private final GatewayImpl gateway;
    private final Channel channel;
    private final BlockEventSource blockSource;

    public ReplayListenerSession(NetworkImpl network, Consumer<BlockEvent> listener, long startBlock) {
        gateway = network.getGateway().newInstance();
        String channelName = network.getChannel().getName();
        channel = gateway.getNetwork(channelName).getChannel();

        // Remove old peers first to avoid receiving spurious events from them
        Collection<Peer> eventingPeers = channel.getPeers(EnumSet.of(Peer.PeerRole.EVENT_SOURCE));
        removeAllPeers();

        // Attach listener before replay peers to ensure no replay events are missed
        BlockEventSource channelBlockSource = BlockEventSourceFactory.getInstance().newBlockEventSource(channel);
        blockSource = new OrderedBlockEventSource(channelBlockSource, startBlock);
        blockSource.addBlockListener(listener);

        addReplayPeers(eventingPeers, startBlock);
    }

    private void removeAllPeers() {
        try {
            for (Peer peer : channel.getPeers()) {
                channel.removePeer(peer);
            }
        } catch (InvalidArgumentException e) {
            throw new GatewayRuntimeException("Failed to remove peers from channel", e);
        }
    }

    private void addReplayPeers(Collection<Peer> eventingPeers, long startBlock) {
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
            throw new GatewayRuntimeException("Failed to add peers for event replay", e);
        }
    }

    @Override
    public void close() {
        blockSource.close();
        gateway.close();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '@' + System.identityHashCode(this) +
                "(channel=" + channel +
                ", blockSource=" + blockSource + ')';
    }
}
