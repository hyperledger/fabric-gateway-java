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
import org.hyperledger.fabric.gateway.impl.event.StubBlockEventSource;
import org.hyperledger.fabric.gateway.spi.BlockListener;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.Peer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class NetworkTest {
    private static final TestUtils testUtils = TestUtils.getInstance();

    private Gateway gateway = null;
    private Network network = null;
    private StubBlockEventSource stubBlockEventSource = null;
    private final Peer peer1 = testUtils.newMockPeer("peer1");
    private final Peer peer2 = testUtils.newMockPeer("peer2");

    @BeforeEach
    public void beforeEach() throws Exception {
        stubBlockEventSource = new StubBlockEventSource(); // Must be before network is created
        gateway = testUtils.newGatewayBuilder().connect();
        network = gateway.getNetwork("ch1");
    }

    @AfterEach
    public void afterEach() {
        stubBlockEventSource.close();
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

    @Test
    public void testAddBlockListenerReturnsTheBlockListener() {
        BlockListener listener = blockEvent -> {};

        BlockListener result = network.addBlockListener(listener);

        assertThat(result).isSameAs(listener);
    }

    @Test
    public void testBlockListenerReceivesBlockEvents() {
        BlockListener listener = Mockito.mock(BlockListener.class);
        BlockEvent event = testUtils.newMockBlockEvent(peer1, 2);

        network.addBlockListener(listener);
        stubBlockEventSource.sendEvent(event);

        Mockito.verify(listener).receivedBlock(event);
    }

    @Test
    public void testRemovedBlockListenerDoesNotReceiveEvents() {
        BlockListener listener = Mockito.mock(BlockListener.class);
        BlockEvent event = testUtils.newMockBlockEvent(peer1, 1);

        network.addBlockListener(listener);
        network.removeBlockListener(listener);
        stubBlockEventSource.sendEvent(event);

        Mockito.verify(listener, Mockito.never()).receivedBlock(event);
    }

    @Test
    public void testBlockListenerDoesNotReceiveDuplicateEvents() {
        BlockListener listener = Mockito.mock(BlockListener.class);
        BlockEvent event = testUtils.newMockBlockEvent(peer1, 1);
        BlockEvent duplicateEvent = testUtils.newMockBlockEvent(peer2, 1);

        network.addBlockListener(listener);
        stubBlockEventSource.sendEvent(event);
        stubBlockEventSource.sendEvent(duplicateEvent);

        Mockito.verify(listener, Mockito.never()).receivedBlock(duplicateEvent);
    }

    @Test
    public void testBlockListerReceivesEventsInOrder() {
        BlockListener listener = Mockito.mock(BlockListener.class);
        BlockEvent event1 = testUtils.newMockBlockEvent(peer1, 1);
        BlockEvent event2 = testUtils.newMockBlockEvent(peer1, 2);
        BlockEvent event3 = testUtils.newMockBlockEvent(peer1, 3);

        network.addBlockListener(listener);
        stubBlockEventSource.sendEvent(event1); // Prime the listener with an initial block number
        stubBlockEventSource.sendEvent(event3);
        stubBlockEventSource.sendEvent(event2);

        InOrder orderVerifier = Mockito.inOrder(listener);
        orderVerifier.verify(listener).receivedBlock(event1);
        orderVerifier.verify(listener).receivedBlock(event2);
        orderVerifier.verify(listener).receivedBlock(event3);
    }

    @Test
    public void testBlockListerDoesNotReceiveOldEvents() {
        BlockListener listener = Mockito.mock(BlockListener.class);
        BlockEvent event1 = testUtils.newMockBlockEvent(peer1, 1);
        BlockEvent event2 = testUtils.newMockBlockEvent(peer1, 2);

        network.addBlockListener(listener);
        stubBlockEventSource.sendEvent(event2);
        stubBlockEventSource.sendEvent(event1);

        Mockito.verify(listener, Mockito.never()).receivedBlock(event1);
    }

    @Test
    public void testBlockListerReceivesNewEventsAfterOldEvents() {
        BlockListener listener = Mockito.mock(BlockListener.class);
        BlockEvent event1 = testUtils.newMockBlockEvent(peer1, 1);
        BlockEvent event2 = testUtils.newMockBlockEvent(peer1, 2);
        BlockEvent event3 = testUtils.newMockBlockEvent(peer1, 3);

        network.addBlockListener(listener);
        stubBlockEventSource.sendEvent(event2); // Prime the listener with an initial block number
        stubBlockEventSource.sendEvent(event1);
        stubBlockEventSource.sendEvent(event3);

        Mockito.verify(listener).receivedBlock(event3);
    }
}
