/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v1CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcECContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.hyperledger.fabric.gateway.impl.Enrollment;
import org.hyperledger.fabric.gateway.impl.GatewayImpl;
import org.hyperledger.fabric.gateway.impl.event.PeerDisconnectEvent;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.ChaincodeResponse;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ServiceDiscoveryException;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

public final class TestUtils {
    private static final TestUtils INSTANCE = new TestUtils();

    private final Path networkConfigPath = Paths.get("src", "test", "java", "org", "hyperledger", "fabric", "gateway", "connection.json");

    public static TestUtils getInstance() {
        return INSTANCE;
    }

    private TestUtils() { }

    public GatewayImpl.Builder newGatewayBuilder() throws GatewayException, OperatorCreationException, CertificateException, NoSuchAlgorithmException, NoSuchProviderException, IOException {
        Enrollment enrollment = newEnrollment();
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
        AtomicReference<Peer.PeerEventingServiceDisconnected> disconnectHandler =
                new AtomicReference<>(Mockito.mock(Peer.PeerEventingServiceDisconnected.class));

        Peer mockPeer = Mockito.mock(Peer.class);

        Mockito.when(mockPeer.getName()).thenReturn(name);
        Mockito.when(mockPeer.getPeerEventingServiceDisconnected()).thenAnswer(invocation -> disconnectHandler.get());
        Mockito.when(mockPeer.setPeerEventingServiceDisconnected(Mockito.any(Peer.PeerEventingServiceDisconnected.class)))
                .thenAnswer(invocation -> disconnectHandler.getAndSet(invocation.getArgument(0)));

        return mockPeer;
    }

    public Channel newMockChannel(String name) {
        Channel mockChannel = Mockito.mock(Channel.class);
        Mockito.when(mockChannel.getName()).thenReturn(name);

        AtomicReference<Channel.SDPeerAddition> sdPeerAdditionRef = new AtomicReference<>(newMockSDPeerAddition());
        Mockito.when(mockChannel.getSDPeerAddition())
                .thenAnswer(invocation -> sdPeerAdditionRef.get());
        Mockito.when(mockChannel.setSDPeerAddition(Mockito.any(Channel.SDPeerAddition.class)))
                .thenAnswer(invocation -> sdPeerAdditionRef.getAndSet(invocation.getArgument(0)));

        return mockChannel;
    }

    private Channel.SDPeerAddition newMockSDPeerAddition() {
        Channel.SDPeerAddition mockPeerAddition = Mockito.mock(Channel.SDPeerAddition.class);
        try {
            Mockito.when(mockPeerAddition.addPeer(Mockito.any(Channel.SDPeerAdditionInfo.class)))
                    .thenAnswer(invocation -> newMockPeer("mockPeer"));
        } catch (ServiceDiscoveryException | InvalidArgumentException e) {
            throw new RuntimeException(e);
        }
        return mockPeerAddition;
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

    public BlockEvent newMockBlockEvent(Peer peer, long blockNumber, BlockEvent.TransactionEvent... transactionEvents) {
        BlockEvent mockEvent = Mockito.mock(BlockEvent.class);
        Mockito.when(mockEvent.getPeer()).thenReturn(peer);
        Mockito.when(mockEvent.getBlockNumber()).thenReturn(blockNumber);
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

    public Enrollment newEnrollment() throws OperatorCreationException, IOException, NoSuchAlgorithmException, NoSuchProviderException, CertificateException {
        Provider bcProvider = new BouncyCastleProvider();
        Security.addProvider(bcProvider);
        // GENERATE THE PUBLIC/PRIVATE RSA KEY PAIR
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC", "BC");
        generator.initialize(384);
        KeyPair keyPair = generator.generateKeyPair();
        // GENERATE THE X509 CERTIFICATE
        X500Name dnName = new X500Name("CN=John Doe");
        // yesterday
        Date validityBeginDate = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
        // in 2 years
        Date validityEndDate = new Date(System.currentTimeMillis() + 2 * 365 * 24 * 60 * 60 * 1000);
        SubjectPublicKeyInfo subPubKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());
        X509v1CertificateBuilder builder = new X509v1CertificateBuilder(
                dnName,
                BigInteger.valueOf(System.currentTimeMillis()),
                validityBeginDate,
                validityEndDate,
                Locale.getDefault(),
                dnName,
                subPubKeyInfo);

        AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find("SHA256WithRSAEncryption");
        AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);
        ContentSigner contentSigner = new BcECContentSignerBuilder(sigAlgId, digAlgId)
                        .build(PrivateKeyFactory.createKey(keyPair.getPrivate().getEncoded()));

        X509CertificateHolder holder = builder.build(contentSigner);

        X509Certificate cert = new JcaX509CertificateConverter().getCertificate(holder);

        StringWriter string = new StringWriter();
        try(PemWriter pemWriter = new PemWriter(string)) {
            PemObject pemObject = new PemObject("CERTIFICATE", cert.getEncoded());
            pemWriter.writeObject(pemObject);
        }
        String certificate = string.toString();
        PrivateKey pk = keyPair.getPrivate();
        return new Enrollment() {

            @Override
            public String getCertificate() {
                return certificate;
            }

            @Override
            public PrivateKey getPrivateKey() {
                return pk;
            }
        };
    }
}
