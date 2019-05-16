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
import org.hyperledger.fabric.gateway.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        assertThat(txn.getName()).isEqualTo("txn1");
    }

    @Test
    public void testCreateTransactionWithNamespace() {
        Transaction txn = network.getContract("contract2", "name1").createTransaction("txn2");
        assertThat(txn.getName()).isEqualTo("name1:txn2");
    }

    @Test
    public void testCreateTransactionWithEmptyNameThrows() {
        Contract contract = network.getContract("contract");
        assertThatThrownBy(() -> contract.createTransaction(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testCreateTransactionWithNullNameThrows() {
        Contract contract = network.getContract("contract");
        assertThatThrownBy(() -> contract.createTransaction(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
