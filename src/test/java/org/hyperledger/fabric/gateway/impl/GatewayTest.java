/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;

import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.GatewayException;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallet.Identity;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class GatewayTest {
  Gateway.Builder builder = null;
  static Path networkConfigPath = null;
  static PrivateKey pk;
  static String certificate;

  @BeforeClass
  public static void initialize() throws Exception {
	Enrollment enrollment = Enrollment.createTestEnrollment();
	pk = enrollment.getPrivateKey();
	certificate = enrollment.getCertificate();
    networkConfigPath = Paths.get("src", "test", "java", "org", "hyperledger", "fabric", "gateway", "connection.json");
  }

  @Before
  public void setup() throws Exception {
    builder = Gateway.createBuilder();
    Wallet wallet = Wallet.createInMemoryWallet();
    wallet.put("user", Identity.createIdentity("msp1", certificate, pk));
    builder.identity(wallet, "user").networkConfig(networkConfigPath);
  }

  @Test
  public void testGetNetwork() throws Exception {
    try (Gateway gateway = builder.connect()) {
      Network network = gateway.getNetwork("mychannel");
      Assert.assertEquals(((NetworkImpl)network).getChannel().getName(), "mychannel");
    } catch (GatewayException e) {
    	// expect this to throw when attempting to initialise channel without fabric network stood up
    }
  }

  @Test
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
    builder.networkConfig(networkConfigPath);
    try (GatewayImpl gateway = (GatewayImpl) builder.connect()) {
      Network network = gateway.getNetwork("mychannel");
      Assert.assertEquals(((NetworkImpl)network).getChannel().getName(), "mychannel");
    } catch (GatewayException e) {
    	// expect this to throw when attempting to initialise channel without fabric network stood up
    }
  }

  @Test
  public void testGetAssumedNetwork() throws Exception {
    try (Gateway gateway = builder.connect()) {
      Network network = gateway.getNetwork("assumed");
      Assert.assertEquals(((NetworkImpl)network).getChannel().getName(), "assumed");
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
