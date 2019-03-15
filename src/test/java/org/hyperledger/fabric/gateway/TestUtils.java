/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway;

import org.bouncycastle.operator.OperatorCreationException;
import org.hyperledger.fabric.gateway.impl.Enrollment;
import org.hyperledger.fabric.gateway.impl.GatewayImpl;
import org.hyperledger.fabric.gateway.impl.event.PeerDisconnectEvent;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.Peer;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.util.Arrays;

public final class TestUtils {
    private static final TestUtils INSTANCE = new TestUtils();

    private final Path networkConfigPath = Paths.get("src", "test", "java", "org", "hyperledger", "fabric", "gateway", "connection.json");

    public static TestUtils getInstance() {
        return INSTANCE;
    }

    private TestUtils() { }

    public GatewayImpl.Builder newGatewayBuilder() throws GatewayException, OperatorCreationException, CertificateException, NoSuchAlgorithmException, NoSuchProviderException, IOException {
        Enrollment enrollment = Enrollment.createTestEnrollment();
        PrivateKey privateKey = enrollment.getPrivateKey();
        String certificate = enrollment.getCertificate();

        GatewayImpl.Builder builder = (GatewayImpl.Builder)Gateway.createBuilder();
        Wallet wallet = Wallet.createInMemoryWallet();
        wallet.put("user", Wallet.Identity.createIdentity("msp1", certificate, privateKey));
        builder.identity(wallet, "user").networkConfig(networkConfigPath);
        return builder;
    }

    public Peer newMockPeer(String name) {
        Peer mockPeer = Mockito.mock(Peer.class);
        Mockito.doReturn(name).when(mockPeer).getName();
        return mockPeer;
    }

    public BlockEvent.TransactionEvent newValidMockTransactionEvent(Peer peer, String transactionId) {
        BlockEvent.TransactionEvent txEvent = newMockTransactionEvent(peer, transactionId);
        Mockito.when(txEvent.isValid()).thenReturn(true);
        return txEvent;
    }

    public BlockEvent.TransactionEvent newInvalidMockTransactionEvent(Peer peer, String transactionId) {
        BlockEvent.TransactionEvent txEvent = newMockTransactionEvent(peer, transactionId);
        Mockito.when(txEvent.isValid()).thenReturn(false);
        return txEvent;
    }

    private BlockEvent.TransactionEvent newMockTransactionEvent(Peer peer, String transactionId) {
        BlockEvent.TransactionEvent txEvent = Mockito.mock(BlockEvent.TransactionEvent.class);
        Mockito.when(txEvent.getPeer()).thenReturn(peer);
        Mockito.when(txEvent.getTransactionID()).thenReturn(transactionId);
        return txEvent;
    }

    public BlockEvent newMockBlockEvent(Peer peer, BlockEvent.TransactionEvent... transactionEvents) {
        BlockEvent mockEvent = Mockito.mock(BlockEvent.class);
        Mockito.when(mockEvent.getPeer()).thenReturn(peer);
        Mockito.when(mockEvent.getTransactionEvents()).thenReturn(Arrays.asList(transactionEvents));
        return mockEvent;
    }

    public PeerDisconnectEvent newPeerDisconnectedEvent(Peer peer) {
        return new PeerDisconnectEvent() {
            @Override
            public Peer getPeer() {
                return peer;
            }

            @Override
            public Throwable getCause() {
                return null;
            }
        };
    }
}
