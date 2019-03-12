/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.GatewayException;
import org.hyperledger.fabric.gateway.Transaction;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallet.Identity;
import org.hyperledger.fabric.sdk.Channel;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ContractTest {
  Channel channel = null;
  Gateway gateway = null;
  NetworkImpl network = null;
  Contract contract = null;
  Contract nscontract = null;
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
    network = (NetworkImpl) gateway.getNetwork("ch1");
    contract = network.getContract("contract1");
    nscontract = network.getContract("contract2", "name1");
  }

  @Test
  public void testCreateTransaction() {
    Transaction txn = contract.createTransaction("txn1");
    Assert.assertEquals(txn.getName(), "txn1");
  }

  @Test
  public void testCreateTransactionWithNamespace() {
    Transaction txn = nscontract.createTransaction("txn2");
    Assert.assertEquals(txn.getName(), "name1:txn2");
  }

  @Test
  public void testSubmitTransaction() throws Exception {
    Contract mockContract = new MockContract(network, "contract1", "");
    byte[] result = mockContract.submitTransaction("txn1", "arg1");
    Assert.assertEquals(new String(result), "success");
  }

  @Test
  public void testEvaluateTransaction() throws Exception {
    Contract mockContract = new MockContract(network, "contract1", "");
    byte[] result = mockContract.evaluateTransaction("txn1", "arg1");
    Assert.assertEquals(new String(result), "results");
  }

  class MockContract extends ContractImpl {

    MockContract(NetworkImpl network, String chaincodeId, String name) {
      super(network, chaincodeId, name);
    }

    @Override
    public Transaction createTransaction(String name) {
      return new Transaction() {

        @Override
        public byte[] submit(String... args) throws GatewayException, TimeoutException {
          return "success".getBytes();
        }

        @Override
        public void setTransient(Map<String, byte[]> transientData) {
        }

        @Override
        public String getTransactionId() {
          return null;
        }

        @Override
        public String getName() {
          return name;
        }

        @Override
        public byte[] evaluate(String... args) throws GatewayException {
          return "results".getBytes();
        }

      };
    }

  }
}
