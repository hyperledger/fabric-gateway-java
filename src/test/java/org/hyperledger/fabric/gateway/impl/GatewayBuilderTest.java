/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.stream.Collectors;

import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Identities;
import org.hyperledger.fabric.gateway.Identity;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallets;
import org.hyperledger.fabric.gateway.X509Credentials;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.Peer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GatewayBuilderTest {
    private static final Path CONFIG_PATH = Paths.get("src", "test", "java", "org", "hyperledger", "fabric", "gateway");
    private static final Path JSON_NETWORK_CONFIG_PATH = CONFIG_PATH.resolve("connection.json");
    private static final Path YAML_NETWORK_CONFIG_PATH = CONFIG_PATH.resolve("connection.yaml");

    private final X509Credentials credentials = new X509Credentials();
    private final Identity identity = Identities.newX509Identity("msp1", credentials.getCertificate(), credentials.getPrivateKey());
    private Gateway.Builder builder;
    private Wallet testWallet;

    @BeforeEach
    public void setup() throws IOException {
        builder = Gateway.createBuilder();
        builder.queryHandler(network -> (query -> null)); // Prevent failure if networks are created

        testWallet = Wallets.newInMemoryWallet();
        testWallet.put("admin", identity);
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
    public void testBuilderInvalidIdentity() {
        assertThatThrownBy(() -> builder.identity(testWallet, "INVALID_IDENTITY"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("INVALID_IDENTITY");
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
            assertThat(gateway.getIdentity()).isEqualTo(identity);
            HFClient client = ((GatewayImpl) gateway).getClient();
            assertThat(client.getUserContext().getEnrollment().getCert()).isEqualTo(credentials.getCertificatePem());
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
    public void testBuilderForUnsupportedIdentityType() throws IOException {
        Identity unsupportedIdentity = new Identity() {
            @Override
            public String getMspId() {
                return "mspId";
            }
        };
        assertThatThrownBy(() -> builder.identity(unsupportedIdentity))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(unsupportedIdentity.getClass().getName());
    }

    @Test
    public void testBuilderWithoutIdentity() throws IOException {
        assertThatThrownBy(() -> builder.identity(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Identity must not be null");
    }

    @Test
    public void testBuilderWithIdentity() throws IOException {
        builder.identity(identity)
                .networkConfig(JSON_NETWORK_CONFIG_PATH);
        try (Gateway gateway = builder.connect()) {
            assertThat(gateway.getIdentity()).isEqualTo(identity);
            HFClient client = ((GatewayImpl) gateway).getClient();
            assertThat(client.getUserContext().getEnrollment().getCert()).isEqualTo(credentials.getCertificatePem());
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
