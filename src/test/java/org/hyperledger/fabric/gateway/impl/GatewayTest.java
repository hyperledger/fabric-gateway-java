/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.TestUtils;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class GatewayTest {
    private static final TestUtils testUtils = TestUtils.getInstance();

    private GatewayImpl.Builder builder = null;

    @BeforeEach
    public void beforeEach() throws Exception {
        builder = testUtils.newGatewayBuilder();
    }

    @Test
    public void testGetNetworkFromConfig() {
        try (Gateway gateway = builder.connect()) {
            Network network = gateway.getNetwork("mychannel");
            assertThat(network.getChannel().getName()).isEqualTo("mychannel");
        }
    }

    @Test
    public void testGetAssumedNetwork() {
        try (Gateway gateway = builder.connect()) {
            Network network = gateway.getNetwork("assumed");
            assertThat(network.getChannel().getName()).isEqualTo("assumed");
        }
    }

    @Test
    public void testGetCachedNetwork() {
        try (Gateway gateway = builder.connect()) {
            Network network = gateway.getNetwork("assumed");
            Network network2 = gateway.getNetwork("assumed");
            assertThat(network).isSameAs(network2);
        }
    }

    @Test
    public void testGetNetworkEmptyString() {
        try (Gateway gateway = builder.connect()) {
            assertThatThrownBy(() -> gateway.getNetwork(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Channel name must be a non-empty string");
        }
    }

    @Test
    public void testGetNetworkNullString() {
        try (Gateway gateway = builder.connect()) {
            assertThatThrownBy(() -> gateway.getNetwork(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Channel name must be a non-empty string");
        }
    }

    @Test
    public void testCloseGatewayClosesNetworks() {
        Gateway gateway = builder.connect();
        Channel channel = gateway.getNetwork("assumed").getChannel();

        gateway.close();

        assertThat(channel.isShutdown()).isTrue();
    }

    @Test
    public void testCloseGatewayForceClosesNetworks() {
        HFClient mockClient = testUtils.newMockClient();
        Channel mockChannel = testUtils.newMockChannel("CHANNEL");
        Mockito.when(mockClient.getChannel("CHANNEL")).thenReturn(mockChannel);

        builder.client(mockClient);

        try (Gateway gateway = builder.connect()) {
            gateway.getNetwork("CHANNEL");
        }

        Mockito.verify(mockChannel).shutdown(true);
    }

    @Test
    public void testCloseGatewayWithForceCloseDisabledClosesNetworks() {
        HFClient mockClient = testUtils.newMockClient();
        Channel mockChannel = testUtils.newMockChannel("CHANNEL");
        Mockito.when(mockClient.getChannel("CHANNEL")).thenReturn(mockChannel);

        builder.client(mockClient);
        builder.forceClose(false);

        try (Gateway gateway = builder.connect()) {
            gateway.getNetwork("CHANNEL");
        }

        Mockito.verify(mockChannel).shutdown(false);
    }

    @Test
    public void testNewInstanceHasSameCryptoSuite() {
        final HFClient clientSpy;
        try (GatewayImpl gateway = builder.connect()) {
            clientSpy = Mockito.spy(gateway.getClient());
        }

        // Mock the CryptoSuite so it isn't the default CryptoSuite
        Mockito.when(clientSpy.getCryptoSuite()).thenReturn(Mockito.mock(CryptoSuite.class));

        try (GatewayImpl gateway = builder.client(clientSpy).connect()) {
            GatewayImpl copy = gateway.newInstance();

            assertThat(copy.getClient().getCryptoSuite()).isSameAs(gateway.getClient().getCryptoSuite());
        }
    }

    @Test
    public void testNewInstanceHasSameUserContext() {
        try (GatewayImpl gateway = builder.connect()) {
            GatewayImpl copy = gateway.newInstance();

            assertThat(copy.getClient().getUserContext()).isSameAs(gateway.getClient().getUserContext());
        }
    }

    @Test
    public void testNewInstanceHasSameExecutorService() {
        try (GatewayImpl gateway = builder.connect()) {
            GatewayImpl copy = gateway.newInstance();

            assertThat(copy.getClient().getExecutorService()).isSameAs(gateway.getClient().getExecutorService());
        }
    }
}
