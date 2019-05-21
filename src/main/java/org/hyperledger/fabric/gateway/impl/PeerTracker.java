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

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

/**
 * Track additional configuration information for network peers.
 */
final class PeerTracker {
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
        Map<String, Peer> channelPeersByName = channel.getPeers().stream()
                .collect(Collectors.toMap(Peer::getName, peer -> peer));

        for (NetworkConfig.OrgInfo orgInfo : networkConfig.getOrganizationInfos()) {
            String mspId = orgInfo.getMspId();
            for (String peerName : orgInfo.getPeerNames()) {
                Peer peer = channelPeersByName.get(peerName);
                if (peer != null) { // Ignore peers not in this channel
                    peerOrgMap.putIfAbsent(peer, mspId);
                }
            }
        }
    }

    public String getPeerOrganization(Peer peer) {
        return peerOrgMap.get(peer);
    }
}
