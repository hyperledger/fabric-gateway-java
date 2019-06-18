/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.GatewayException;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.TestUtils;
import org.hyperledger.fabric.sdk.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class GatewayTest {
    private static final TestUtils testUtils = TestUtils.getInstance();

    private Gateway.Builder builder = null;

    @BeforeEach
    public void beforeEach() throws Exception {
        builder = testUtils.newGatewayBuilder();
    }

    @Test
    public void testGetNetworkFromConfig() throws GatewayException {
        try (Gateway gateway = builder.connect()) {
            Network network = gateway.getNetwork("mychannel");
            assertThat(network.getChannel().getName()).isEqualTo("mychannel");
        }
    }

    @Test
    public void testGetAssumedNetwork() throws Exception {
        try (Gateway gateway = builder.connect()) {
            Network network = gateway.getNetwork("assumed");
            assertThat(network.getChannel().getName()).isEqualTo("assumed");
        }
    }

    @Test
    public void testGetCachedNetwork() throws GatewayException {
        try (Gateway gateway = builder.connect()) {
            Network network = gateway.getNetwork("assumed");
            Network network2 = gateway.getNetwork("assumed");
            assertThat(network).isSameAs(network2);
        }
    }

    @Test
    public void testGetNetworkEmptyString() throws Exception {
        try (Gateway gateway = builder.connect()) {
            assertThatThrownBy(() -> gateway.getNetwork(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Channel name must be a non-empty string");
        }
    }

    @Test
    public void testGetNetworkNullString() throws Exception {
        try (Gateway gateway = builder.connect()) {
            assertThatThrownBy(() -> gateway.getNetwork(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Channel name must be a non-empty string");
        }
    }

    @Test
    public void testCloseGatewayClosesNetworks() throws Exception {
        Gateway gateway = builder.connect();
        Channel channel = gateway.getNetwork("assumed").getChannel();

        gateway.close();

        assertThat(channel.isShutdown()).isTrue();
    }
}
