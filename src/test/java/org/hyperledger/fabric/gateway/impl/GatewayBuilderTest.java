/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.GatewayException;
import org.hyperledger.fabric.gateway.TestUtils;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class GatewayBuilderTest {
    Gateway.Builder builder = null;
    static Path networkConfigPath = null;
    static Enrollment enrollment = null;

    @BeforeAll
    public static void enroll() throws Exception {
        enrollment = TestUtils.getInstance().newEnrollment();
        networkConfigPath = Paths.get("src", "test", "java", "org", "hyperledger", "fabric", "gateway", "connection.json");
    }

    @BeforeEach
    public void setup() {
        builder = Gateway.createBuilder();
    }

    @Test
    public void testBuilderNoOptions() {
        assertThatThrownBy(() -> builder.connect())
                .isInstanceOf(GatewayException.class)
                .hasMessage("The gateway identity must be set");
    }

    @Test
    public void testBuilderNoCcp() throws GatewayException {
        Wallet wallet = Wallet.createInMemoryWallet();
        wallet.put("admin", Wallet.Identity.createIdentity("msp1", enrollment.getCertificate(), enrollment.getPrivateKey()));
        builder.identity(wallet, "admin");
        assertThatThrownBy(() -> builder.connect())
                .isInstanceOf(GatewayException.class).hasMessage("The network configuration must be specified");
    }

    @Test
    public void testBuilderInvalidIdentity() throws GatewayException {
        Wallet wallet = Wallet.createInMemoryWallet();
        wallet.put("admin", Wallet.Identity.createIdentity("msp1", "cert", null));
        builder.identity(wallet, "admin").networkConfig(networkConfigPath);
        assertThatThrownBy(() -> builder.connect())
                .isInstanceOf(GatewayException.class)
                .hasCauseInstanceOf(InvalidArgumentException.class);
    }

    @Test
    public void testBuilderYamlCcp() throws GatewayException {
        Wallet wallet = Wallet.createInMemoryWallet();
        wallet.put("admin", Wallet.Identity.createIdentity("msp1", enrollment.getCertificate(), enrollment.getPrivateKey()));
        builder.identity(wallet, "admin");
        builder.queryHandler(network -> (query -> null));
        Path yamlPath = Paths.get("src", "test", "java", "org", "hyperledger", "fabric", "gateway", "connection.yaml");
        builder.networkConfig(yamlPath);
        try (Gateway gateway = builder.connect()) {
            Collection<String> peerNames = gateway.getNetwork("mychannel").getChannel().getPeers().stream()
                    .map(Peer::getName)
                    .collect(Collectors.toList());
            assertThat(peerNames).containsExactly("peer0.org1.example.com");
        }
    }

    @Test
    public void testBuilderInvalidCcp() throws GatewayException {
        Wallet wallet = Wallet.createInMemoryWallet();
        wallet.put("admin", Wallet.Identity.createIdentity("msp1", enrollment.getCertificate(), enrollment.getPrivateKey()));
        builder.identity(wallet, "admin");
        assertThatThrownBy(() -> builder.networkConfig(Paths.get("invalidPath")))
                .isInstanceOf(GatewayException.class)
                .hasCauseInstanceOf(IOException.class);
    }

    @Test
    public void testBuilderWithWalletIdentity() throws GatewayException {
        Wallet wallet = Wallet.createInMemoryWallet();
        wallet.put("admin", Wallet.Identity.createIdentity("msp1", enrollment.getCertificate(), enrollment.getPrivateKey()));
        builder.identity(wallet, "admin").networkConfig(networkConfigPath);
        try (Gateway gateway = builder.connect()) {
            assertThat(gateway.getIdentity().getCertificate()).isEqualTo(enrollment.getCertificate());
            HFClient client = ((GatewayImpl) gateway).getClient();
            assertThat(client.getUserContext().getEnrollment().getCert()).isEqualTo(enrollment.getCertificate());
        }
    }
}
