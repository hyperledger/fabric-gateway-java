/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.TestUtils;
import org.hyperledger.fabric.gateway.Transaction;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ContractTest {
    private Network network;

    @BeforeEach
    public void beforeEach() throws Exception {
        Gateway gateway = TestUtils.getInstance().newGatewayBuilder().connect();
        network = gateway.getNetwork("ch1");
    }

    @Test
    public void testCreateTransaction() {
        Transaction txn = network.getContract("contract1").createTransaction("txn1");
        Assert.assertEquals(txn.getName(), "txn1");
    }

    @Test
    public void testCreateTransactionWithNamespace() {
        Transaction txn = network.getContract("contract2", "name1").createTransaction("txn2");
        Assert.assertEquals(txn.getName(), "name1:txn2");
    }

//  @Test
//  public void testSubmitTransaction() throws Exception {
//    Contract mockContract = new MockContract(network, "contract1", "");
//    byte[] result = mockContract.submitTransaction("txn1", "arg1");
//    Assert.assertEquals(new String(result), "success");
//  }
//
//  @Test
//  public void testEvaluateTransaction() throws Exception {
//    Contract mockContract = new MockContract(network, "contract1", "");
//    byte[] result = mockContract.evaluateTransaction("txn1", "arg1");
//    Assert.assertEquals(new String(result), "results");
//  }
//
//  class MockContract extends ContractImpl {
//
//    MockContract(NetworkImpl network, String chaincodeId, String name) {
//      super(network, chaincodeId, name);
//    }
//
//    @Override
//    public Transaction createTransaction(String name) {
//      return new Transaction() {
//
//        @Override
//        public byte[] submit(String... args) throws GatewayException, TimeoutException {
//          return "success".getBytes();
//        }
//
//        @Override
//        public void setTransient(Map<String, byte[]> transientData) {
//        }
//
//        @Override
//        public String getName() {
//          return name;
//        }
//
//        @Override
//        public byte[] evaluate(String... args) throws GatewayException {
//          return "results".getBytes();
//        }
//
//      };
//    }
//
//  }
}
