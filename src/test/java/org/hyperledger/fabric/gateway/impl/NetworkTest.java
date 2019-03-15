/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.TestUtils;
import org.hyperledger.fabric.sdk.Channel;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

public class NetworkTest {
    private Gateway gateway = null;
    private Network network = null;

    @BeforeEach
    public void beforeEach() throws Exception {
        gateway = TestUtils.getInstance().newGatewayBuilder().connect();
        network = gateway.getNetwork("ch1");
    }

    @Test
    public void testGetChannel() {
        Channel ch1 = ((NetworkImpl) network).getChannel();
        Assert.assertEquals(ch1.getName(), "ch1");
    }

    @Test
    public void testGetGateway() {
        Gateway gw = network.getGateway();
        Assert.assertEquals(gw, gateway);
    }

    @org.junit.jupiter.api.Test
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

    @org.junit.jupiter.api.Test
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

    @org.junit.jupiter.api.Test
    public void testGetContractNullName() {
        try {
            network.getContract("id", null);
        } catch (IllegalArgumentException e) {
            Assert.assertEquals(e.getMessage(), "getContract: name must not be null");
        }
    }

//    @Test
//    public void testGetTransactionEventSource() {
//        BlockEvent.TransactionEvent txEvent = Mockito.mock(BlockEvent.TransactionEvent.class);
//
//        BlockEvent blockEvent = Mockito.mock(BlockEvent.class);
//        Mockito.doReturn(Arrays.asList(txEvent)).when(blockEvent).getTransactionEvents();
//
//        TransactionListener listener = Mockito.mock(TransactionListener.class);
//
//        try (StubBlockEventSource stubBlockSource = new StubBlockEventSource()) {
//            TransactionEventSource transactionSource = network.getTransactionEventSource();
//            transactionSource.addTransactionListener(listener);
//            stubBlockSource.sendEvent(blockEvent);
//        }
//
//        Mockito.verify(listener).receivedTransaction(txEvent);
//    }
}
