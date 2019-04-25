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
import org.hyperledger.fabric.sdk.ChaincodeResponse;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.EnumSet;

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
        builder.identity(wallet, "user")
                .networkConfig(networkConfigPath)
                // Simple query handler so things work out-of-the-box
                .queryHandler(network -> (query -> {
                    Peer peer = network.getChannel().getPeers(EnumSet.of(Peer.PeerRole.CHAINCODE_QUERY)).iterator().next();
                    ProposalResponse response = query.evaluate(peer);
                    if (!response.getStatus().equals(ChaincodeResponse.Status.SUCCESS)) {
                        throw new GatewayException(response.getMessage());
                    }
                    return response;
                }));
        return builder;
    }

    public Peer newMockPeer(String name) {
        Peer mockPeer = Mockito.mock(Peer.class);
        Mockito.doReturn(name).when(mockPeer).getName();
        return mockPeer;
    }

    public Channel newMockChannel(String name) {
        Channel mockChannel = Mockito.mock(Channel.class);
        Mockito.doReturn(name).when(mockChannel).getName();
        return mockChannel;
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

    public ProposalResponse newSuccessfulProposalResponse(byte[] responsePayload) {
        ProposalResponse response = newProposalResponse(responsePayload);
        Mockito.when(response.getStatus()).thenReturn(ChaincodeResponse.Status.SUCCESS);
        return response;
    }

    public ProposalResponse newFailureProposalResponse(String message) {
        ProposalResponse response = newProposalResponse(message.getBytes(StandardCharsets.UTF_8));
        Mockito.when(response.getStatus()).thenReturn(ChaincodeResponse.Status.FAILURE);
        Mockito.when(response.getMessage()).thenReturn(message);
        return response;
    }

    private ProposalResponse newProposalResponse(byte[] responsePayload) {
        ProposalResponse response = Mockito.mock(ProposalResponse.class);
        try {
            Mockito.when(response.getChaincodeActionResponsePayload()).thenReturn(responsePayload);
        } catch (InvalidArgumentException e) {
            throw new RuntimeException(e);
        }
        return response;
    }
}
