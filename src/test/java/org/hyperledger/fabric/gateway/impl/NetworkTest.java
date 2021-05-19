/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.TestUtils;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.Peer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertTrue;

public class NetworkTest {
    private static final TestUtils testUtils = TestUtils.getInstance();

    private Gateway gateway;
    private Gateway gatewayFiltered;
    private Network network;
    private Network networkFiltered;

    @BeforeEach
    public void beforeEach() throws Exception {
        gateway = testUtils.newGatewayBuilder().connect();
        gatewayFiltered = testUtils.newGatewayBuilder().deliverFilter(true).connect();
        network = gateway.getNetwork("ch1");
        networkFiltered = gatewayFiltered.getNetwork("ch1");
    }

    @AfterEach
    public void afterEach() {
        gateway.close();
    }

    @Test
    public void testGetChannel() {
        Channel ch1 = network.getChannel();
        assertThat(ch1.getName()).isEqualTo("ch1");
    }

    @Test
    public void testGetFilteredChannel() {
        Channel ch1 = networkFiltered.getChannel();

        Collection<Peer> peers = ch1.getPeers();
        peers.forEach(peer -> {
            Channel.PeerOptions peerOptions = ch1.getPeersOptions(peer);
            assertTrue(peerOptions.isRegisterEventsForFilteredBlocks());
        });

        assertThat(ch1.getName()).isEqualTo("ch1");
    }


    @Test
    public void testGetGateway() {
        Gateway gw = network.getGateway();
        assertThat(gw).isSameAs(gateway);
    }

    @Test
    public void testGetContract() {
        Contract contract = network.getContract("contract1");
        assertThat(contract).isInstanceOf(ContractImpl.class);
    }

    @Test
    public void testGetCachedContract() {
        Contract contract = network.getContract("contract1");
        Contract contract2 = network.getContract("contract1");
        assertThat(contract).isSameAs(contract2);
    }

    @Test
    public void testGetContractEmptyId() {
        assertThatThrownBy(() -> network.getContract(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("getContract: chaincodeId must be a non-empty string");
    }

    @Test
    public void testGetContractNullId() {
        assertThatThrownBy(() -> network.getContract(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("getContract: chaincodeId must be a non-empty string");
    }

    @Test
    public void testGetContractNullName() {
        assertThatThrownBy(() -> network.getContract("id", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("getContract: name must not be null");
    }

    @Test
    public void testCloseNetworkShutsDownTheChannel() {
        ((NetworkImpl)network).close();
        assertThat(network.getChannel().isShutdown()).isTrue();
    }
}
