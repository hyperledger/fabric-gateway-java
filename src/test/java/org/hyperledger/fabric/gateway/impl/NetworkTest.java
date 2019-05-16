/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.GatewayException;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.TestUtils;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.Peer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class NetworkTest {
    private static final TestUtils testUtils = TestUtils.getInstance();

    private Gateway gateway = null;
    private Network network = null;

    @BeforeEach
    public void beforeEach() throws Exception {
        gateway = testUtils.newGatewayBuilder().connect();
        network = gateway.getNetwork("ch1");
    }

    @Test
    public void testGetChannel() {
        Channel ch1 = network.getChannel();
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
    public void testGetPeerOrganizationForPeerInConnectionProfile() throws GatewayException {
        network = gateway.getNetwork("mychannel");
        Peer peer0 = network.getChannel().getPeers().stream()
                .filter(peer -> "peer0.org1.example.com".equals(peer.getName()))
                .limit(1)
                .collect(Collectors.toList())
                .get(0);

        String result = network.getPeerOrganization(peer0);

        assertThat(result).isEqualTo("Org1MSP");
    }

    @Test
    public void testGetPeerOrganizationForPeerNotInChannelThrows() throws Exception {
        network = gateway.getNetwork("mychannel");
        Peer peer = testUtils.newMockPeer("MockPeer");

        assertThatThrownBy(() -> network.getPeerOrganization(peer))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testGetPeerOrganizationForDiscoveredPeer() throws Exception {
        String channelName = "MockChannel";
        String mspId = "MyMspId";

        Channel mockChannel = testUtils.newMockChannel(channelName);
        HFClient mockClient = Mockito.mock(HFClient.class);
        Mockito.when(mockClient.getChannel(channelName)).thenReturn(mockChannel);

        gateway = testUtils.newGatewayBuilder()
                .client(mockClient)
                .connect();
        network = gateway.getNetwork(channelName);

        // Send fake peer1 discovery event to handler registered by network
        Channel.SDPeerAdditionInfo mockPeerAddInfo = Mockito.mock(Channel.SDPeerAdditionInfo.class);
        Mockito.when(mockPeerAddInfo.getMspId()).thenReturn(mspId);
        Peer newPeer = mockChannel.getSDPeerAddition().addPeer(mockPeerAddInfo);

        String result = network.getPeerOrganization(newPeer);

        assertThat(result).isEqualTo(mspId);
    }
}
