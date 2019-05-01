/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.NetworkConfig;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ServiceDiscoveryException;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

/**
 * Track additional configuration information for network peers.
 */
class PeerTracker {
    private final Channel channel;
    private final Map<Peer, String> peerOrgMap = Collections.synchronizedMap(new WeakHashMap<>());
    private final Channel.SDPeerAddition peerDiscoveryHandler;

    public PeerTracker(Channel channel) {
        this.channel = channel;
        peerDiscoveryHandler = channel.getSDPeerAddition();
        channel.setSDPeerAddition(this::discoveryAddPeer);
    }

    private Peer discoveryAddPeer(Channel.SDPeerAdditionInfo peerAddInfo) throws ServiceDiscoveryException, InvalidArgumentException {
        Peer peer = peerDiscoveryHandler.addPeer(peerAddInfo);
        peerOrgMap.put(peer, peerAddInfo.getMspId());
        return peer;
    }

    public void loadNetworkConfig(NetworkConfig networkConfig) {
        Collection<Peer> channelPeers = channel.getPeers();
        Map<String, Peer> channelPeersByName = new HashMap<>(channelPeers.size());
        channelPeers.forEach(peer -> channelPeersByName.put(peer.getName(), peer));

        networkConfig.getOrganizationInfos().forEach(orgInfo -> {
            String mspId = orgInfo.getMspId();
            orgInfo.getPeerNames().stream()
                    .map(name -> channelPeersByName.get(name))
                    .filter(Objects::nonNull) // Ignore peers not in this channel
                    .forEach(peer -> peerOrgMap.putIfAbsent(peer, mspId));
        });
    }

    public String getPeerOrganization(Peer peer) {
        return peerOrgMap.get(peer);
    }
}
