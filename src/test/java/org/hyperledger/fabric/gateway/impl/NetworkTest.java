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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class NetworkTest {
    private static final TestUtils testUtils = TestUtils.getInstance();

    private Gateway gateway = null;
    private Network network = null;

    @BeforeEach
    public void beforeEach() throws Exception {
        gateway = testUtils.newGatewayBuilder().connect();
        network = gateway.getNetwork("ch1");
    }

    @Test
    public void testGetChannel() {
        Channel ch1 = network.getChannel();
        assertThat(ch1.getName()).isEqualTo("ch1");
    }

    @Test
    public void testGetGateway() {
        Gateway gw = network.getGateway();
        assertThat(gw).isSameAs(gateway);
    }

    @Test
    public void testGetContract() {
        Contract contract = network.getContract("contract1");
        assertThat(contract).isInstanceOf(ContractImpl.class);
    }

    @Test
    public void testGetCachedContract() {
        Contract contract = network.getContract("contract1");
        Contract contract2 = network.getContract("contract1");
        assertThat(contract).isSameAs(contract2);
    }

    @Test
    public void testGetContractEmptyId() {
        assertThatThrownBy(() -> network.getContract(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("getContract: chaincodeId must be a non-empty string");
    }

    @Test
    public void testGetContractNullId() {
        assertThatThrownBy(() -> network.getContract(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("getContract: chaincodeId must be a non-empty string");
    }

    @Test
    public void testGetContractNullName() {
        assertThatThrownBy(() -> network.getContract("id", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("getContract: name must not be null");
    }
}
