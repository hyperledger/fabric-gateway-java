/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.GatewayException;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class GatewayBuilderTest {
  Gateway.Builder builder = null;
  static Path networkConfigPath = null;
  static Enrollment enrollment = null;

  @BeforeAll
  public static void enroll() throws Exception {
    enrollment = Enrollment.createTestEnrollment();
    networkConfigPath = Paths.get("src", "test", "java", "org", "hyperledger", "fabric", "gateway", "connection.json");
  }

  @BeforeEach
  public void setup() {
    builder = Gateway.createBuilder();
  }

  @Test
  public void testBuilderNoOptions() {
    try {
      builder.connect();
      Assert.fail("connect should have thrown");
    } catch (GatewayException e) {
      Assert.assertEquals(e.getMessage(), "The gateway identity must be set");
    }
  }

  @Test
  public void testBuilderNoCcp() {
    try {
      Wallet wallet = Wallet.createInMemoryWallet();
      wallet.put("admin", Wallet.Identity.createIdentity("msp1", enrollment.getCertificate(), enrollment.getPrivateKey()));
      builder.identity(wallet, "admin");
      builder.connect();
      Assert.fail("connect should have thrown");
    } catch (GatewayException e) {
        Assert.assertEquals(e.getMessage(), "The network configuration must be specified");
    }
  }

  @Test
  public void testBuilderInvalidIdentity() {
    try {
      Wallet wallet = Wallet.createInMemoryWallet();
      wallet.put("admin", Wallet.Identity.createIdentity("msp1", "cert", null));
      builder.identity(wallet, "admin").networkConfig(networkConfigPath);
      builder.connect();
      Assert.fail("connect should have thrown");
    } catch (GatewayException e) {
        Assert.assertTrue(e.getCause() instanceof InvalidArgumentException);

    }
  }

  @Test
  public void testBuilderInvalidCcp() {
    try {
      Wallet wallet = Wallet.createInMemoryWallet();
      wallet.put("admin", Wallet.Identity.createIdentity("msp1", enrollment.getCertificate(), enrollment.getPrivateKey()));
      builder.identity(wallet, "admin").networkConfig(Paths.get("invalidPath"));
      builder.connect();
      Assert.fail("connect should have thrown");
    } catch (GatewayException e) {
        Assert.assertTrue(e.getCause() instanceof IOException);

    }
  }

  @Test
  public void testBuilderWithWalletIdentity() throws Exception {
    Wallet wallet = Wallet.createInMemoryWallet();
    wallet.put("admin", Wallet.Identity.createIdentity("msp1", enrollment.getCertificate(), enrollment.getPrivateKey()));
    builder.identity(wallet, "admin").networkConfig(networkConfigPath);
    try (Gateway gateway = builder.connect()) {
        Assert.assertEquals(gateway.getIdentity().getCertificate(), enrollment.getCertificate());
        HFClient client = ((GatewayImpl)gateway).getClient();
        Assert.assertEquals(client.getUserContext().getEnrollment().getCert(), enrollment.getCertificate());
    }
  }
}
