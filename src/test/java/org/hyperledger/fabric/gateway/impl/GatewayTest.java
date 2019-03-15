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
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

public class GatewayTest {
    private Gateway.Builder builder = null;

    @BeforeEach
    public void beforeEach() throws Exception {
        builder = TestUtils.getInstance().newGatewayBuilder();
    }

    @org.junit.jupiter.api.Test
    public void testGetNetwork() throws Exception {
        try (Gateway gateway = builder.connect()) {
            Network network = gateway.getNetwork("mychannel");
            Assert.assertEquals(((NetworkImpl) network).getChannel().getName(), "mychannel");
        } catch (GatewayException e) {
            // expect this to throw when attempting to initialise channel without fabric network stood up
        }
    }

    @org.junit.jupiter.api.Test
    public void testGetCachedNetwork() throws Exception {
        try (Gateway gateway = builder.connect()) {
            Network network = gateway.getNetwork("mychannel");
            Network network2 = gateway.getNetwork("mychannel");
            Assert.assertEquals(network, network2);
        } catch (GatewayException e) {
            // expect this to throw when attempting to initialise channel without fabric network stood up
        }
    }

    @Test
    public void testGetNetworkFromConfig() throws Exception {
        try (GatewayImpl gateway = (GatewayImpl) builder.connect()) {
            Network network = gateway.getNetwork("mychannel");
            Assert.assertEquals(((NetworkImpl) network).getChannel().getName(), "mychannel");
        } catch (GatewayException e) {
            // expect this to throw when attempting to initialise channel without fabric network stood up
        }
    }

    @org.junit.jupiter.api.Test
    public void testGetAssumedNetwork() throws Exception {
        try (Gateway gateway = builder.connect()) {
            Network network = gateway.getNetwork("assumed");
            Assert.assertEquals(((NetworkImpl) network).getChannel().getName(), "assumed");
        }
    }

    @Test
    public void testGetNetworkEmptyString() throws Exception {
        try (Gateway gateway = builder.connect()) {
            gateway.getNetwork("");
            Assert.fail("Should have thrown exception");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals(e.getMessage(), "Channel name must be a non-empty string");
        }
    }

    @Test
    public void testGetNetworkNullString() throws Exception {
        try (Gateway gateway = builder.connect()) {
            gateway.getNetwork(null);
            Assert.fail("Should have thrown exception");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals(e.getMessage(), "Channel name must be a non-empty string");
        }
    }

}
