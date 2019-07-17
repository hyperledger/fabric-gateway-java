/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.stream.Collectors;

import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.TestUtils;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.Peer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class GatewayBuilderTest {
    private static final Path CONFIG_PATH = Paths.get("src", "test", "java", "org", "hyperledger", "fabric", "gateway");
    private static final Path JSON_NETWORK_CONFIG_PATH = CONFIG_PATH.resolve("connection.json");
    private static final Path YAML_NETWORK_CONFIG_PATH = CONFIG_PATH.resolve("connection.yaml");
    private static Enrollment enrollment;

    private Gateway.Builder builder;
    private Wallet testWallet;

    @BeforeAll
    public static void enroll() throws Exception {
        enrollment = TestUtils.getInstance().newEnrollment();
    }

    @BeforeEach
    public void setup() throws IOException {
        builder = Gateway.createBuilder();
        builder.queryHandler(network -> (query -> null)); // Prevent failure if networks are created

        testWallet = Wallet.createInMemoryWallet();
        testWallet.put("admin", Wallet.Identity.createIdentity("msp1", enrollment.getCertificate(), enrollment.getPrivateKey()));
    }

    @Test
    public void testBuilderNoOptions() {
        assertThatThrownBy(() -> builder.connect())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("The gateway identity must be set");
    }

    @Test
    public void testBuilderNoCcp() throws IOException {
        builder.identity(testWallet, "admin");
        assertThatThrownBy(() -> builder.connect())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("The network configuration must be specified");
    }

    @Test
    public void testBuilderInvalidIdentity() throws IOException {
        Wallet emptyWallet = Wallet.createInMemoryWallet();
        builder.identity(emptyWallet, "admin")
                .networkConfig(JSON_NETWORK_CONFIG_PATH);
        assertThatThrownBy(() -> builder.connect())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("The gateway identity must be set");
    }

    @Test
    public void testBuilderYamlCcp() throws IOException {
        builder.identity(testWallet, "admin")
                .networkConfig(YAML_NETWORK_CONFIG_PATH);
        try (Gateway gateway = builder.connect()) {
            Collection<String> peerNames = gateway.getNetwork("mychannel").getChannel().getPeers().stream()
                    .map(Peer::getName)
                    .collect(Collectors.toList());
            assertThat(peerNames).containsExactly("peer0.org1.example.com");
        }
    }

    @Test
    public void testBuilderInvalidCcp() throws IOException {
        builder.identity(testWallet, "admin");
        assertThatThrownBy(() -> builder.networkConfig(Paths.get("invalidPath")))
                .isInstanceOf(IOException.class);
    }

    @Test
    public void testBuilderWithWalletIdentity() throws IOException {
        builder.identity(testWallet, "admin")
                .networkConfig(JSON_NETWORK_CONFIG_PATH);
        try (Gateway gateway = builder.connect()) {
            assertThat(gateway.getIdentity().getCertificate()).isEqualTo(enrollment.getCertificate());
            HFClient client = ((GatewayImpl) gateway).getClient();
            assertThat(client.getUserContext().getEnrollment().getCert()).isEqualTo(enrollment.getCertificate());
        }
    }

    @Test
    public void testYamlStreamNetworkConfig() throws IOException {
        try (InputStream configStream = new FileInputStream(YAML_NETWORK_CONFIG_PATH.toFile())) {
            builder.identity(testWallet, "admin")
                    .networkConfig(configStream);
            try (Gateway gateway = builder.connect()) {
                Collection<String> peerNames = gateway.getNetwork("mychannel").getChannel().getPeers().stream()
                        .map(Peer::getName)
                        .collect(Collectors.toList());
                assertThat(peerNames).containsExactly("peer0.org1.example.com");
            }
        }
    }

    @Test
    public void testJsonStreamNetworkConfig() throws IOException {
        try (InputStream configStream = new FileInputStream(JSON_NETWORK_CONFIG_PATH.toFile())) {
            builder.identity(testWallet, "admin")
                    .networkConfig(configStream);
            try (Gateway gateway = builder.connect()) {
                Collection<String> peerNames = gateway.getNetwork("mychannel").getChannel().getPeers().stream()
                        .map(Peer::getName)
                        .collect(Collectors.toList());
                assertThat(peerNames).containsExactly("peer0.org1.example.com");
            }
        }
    }

    @Test
    public void testFileNetworkConfigReturnsBuilder() throws IOException {
        Gateway.Builder result = builder.networkConfig(JSON_NETWORK_CONFIG_PATH);
        assertThat(result).isSameAs(builder);
    }

    @Test
    public void testStreamNetworkConfigReturnsBuilder() throws IOException {
        try (InputStream configStream = new FileInputStream(JSON_NETWORK_CONFIG_PATH.toFile())) {
            Gateway.Builder result = builder.networkConfig(configStream);
            assertThat(result).isSameAs(builder);
        }
    }
}
