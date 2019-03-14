/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import org.bouncycastle.operator.OperatorCreationException;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.GatewayException;
import org.hyperledger.fabric.gateway.Wallet;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;

public final class TestUtils {
    private static TestUtils instance = null;

    private final PrivateKey pk;
    private final String certificate;
    private final Path networkConfigPath;

    public static synchronized TestUtils instance() throws OperatorCreationException, CertificateException, NoSuchAlgorithmException, NoSuchProviderException, IOException {
        if (instance == null) {
            instance = new TestUtils();
        }
        return instance;
    }

    private TestUtils() throws OperatorCreationException, CertificateException, NoSuchAlgorithmException, NoSuchProviderException, IOException {
        Enrollment enrollment = Enrollment.createTestEnrollment();
        pk = enrollment.getPrivateKey();
        certificate = enrollment.getCertificate();
        networkConfigPath = Paths.get("src", "test", "java", "org", "hyperledger", "fabric", "gateway", "connection.json");
    }

    public GatewayImpl.Builder newGatewayBuilder() throws GatewayException {
        GatewayImpl.Builder builder = (GatewayImpl.Builder)Gateway.createBuilder();
        Wallet wallet = Wallet.createInMemoryWallet();
        wallet.put("user", Wallet.Identity.createIdentity("msp1", certificate, pk));
        builder.identity(wallet, "user").networkConfig(networkConfigPath);
        return builder;
    }
}
