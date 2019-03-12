/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.GatewayException;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallet.Identity;
import org.hyperledger.fabric.sdk.Channel;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class NetworkTest {
  Gateway gateway = null;
  Network network = null;
  static Path networkConfigPath = null;
  static Enrollment enrollment = null;

  @BeforeClass
  public static void enroll() throws Exception {
    enrollment = Enrollment.createTestEnrollment();
    networkConfigPath = Paths.get("src", "test", "java", "org", "hyperledger", "fabric", "gateway", "connection.json");
  }

  @Before
  public void setup() throws Exception {
    Gateway.Builder builder = Gateway.createBuilder();
    Wallet wallet = Wallet.createInMemoryWallet();
    wallet.put("user", Identity.createIdentity("msp1", enrollment.getCertificate(), enrollment.getPrivateKey()));
    builder.identity(wallet, "user").networkConfig(networkConfigPath);
    gateway = builder.connect();
    network = gateway.getNetwork("ch1");
  }

  @Test
  public void testGetChannel() {
    Channel ch1 = ((NetworkImpl)network).getChannel();
    Assert.assertEquals(ch1.getName(), "ch1");
  }

  @Test
  public void testGetGateway() {
    Gateway gw = network.getGateway();
    Assert.assertEquals(gw, gateway);
  }

  @Test
  public void testGetContract() {
    Contract contract = network.getContract("contract1");
    Assert.assertTrue(contract instanceof ContractImpl);
  }

  @Test
  public void testGetCachedContract() {
    Contract contract = network.getContract("contract1");
    Contract contract2 = network.getContract("contract1");
    Assert.assertEquals(contract, contract2);
  }

  @Test
  public void testGetContractEmptyId() {
	try {
	  network.getContract("");
	} catch (IllegalArgumentException e) {
	  Assert.assertEquals(e.getMessage(), "getContract: chaincodeId must be a non-empty string");
	}
  }

  @Test
  public void testGetContractNullId() {
	try {
	  network.getContract(null);
	} catch (IllegalArgumentException e) {
	  Assert.assertEquals(e.getMessage(), "getContract: chaincodeId must be a non-empty string");
	}
  }

  @Test
  public void testGetContractNullName() {
	try {
	  network.getContract("id", null);
	} catch (IllegalArgumentException e) {
	  Assert.assertEquals(e.getMessage(), "getContract: name must not be null");
	}
  }


}
