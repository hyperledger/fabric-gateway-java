/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

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
import org.hyperledger.fabric.gateway.spi.PeerDisconnectEvent;
import org.hyperledger.fabric.protos.peer.FabricProposalResponse;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.ChaincodeResponse;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ServiceDiscoveryException;
import org.mockito.Mockito;

public final class TestUtils {
    private static final TestUtils INSTANCE = new TestUtils();
    private static final String TEST_FILE_PREFIX = "fgj-test-";
    private static final String UNUSED_FILE_PREFIX = "fgj-unused-";
    private static final Path NETWORK_CONFIG_PATH = Paths.get("src", "test", "java", "org", "hyperledger", "fabric", "gateway", "connection.json");

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
                .networkConfig(NETWORK_CONFIG_PATH)
                // Simple query handler so things work out-of-the-box
                .queryHandler(network -> (query -> {
                    Peer peer = network.getChannel().getPeers(EnumSet.of(Peer.PeerRole.CHAINCODE_QUERY)).iterator().next();
                    ProposalResponse response = query.evaluate(peer);
                    if (!response.getStatus().equals(ChaincodeResponse.Status.SUCCESS)) {
                        throw new ContractException(response.getMessage());
                    }
                    return response;
                }));
        return builder;
    }

    public HFClient newMockClient() throws OperatorCreationException, CertificateException, NoSuchAlgorithmException, NoSuchProviderException, IOException {
        Enrollment enrollment = newEnrollment();
        User user = new User() {
            @Override
            public String getName() {
                return "user";
            }

            @Override
            public Set<String> getRoles() {
                return Collections.emptySet();
            }

            @Override
            public String getAccount() {
                return "";
            }

            @Override
            public String getAffiliation() {
                return "";
            }

            @Override
            public org.hyperledger.fabric.sdk.Enrollment getEnrollment() {
                return new org.hyperledger.fabric.sdk.Enrollment() {
                    @Override
                    public PrivateKey getKey() {
                        return enrollment.getPrivateKey();
                    }

                    @Override
                    public String getCert() {
                        return enrollment.getCertificate();
                    }
                };
            }

            @Override
            public String getMspId() {
                return "msp1";
            }
        };

        HFClient mockClient = Mockito.mock(HFClient.class);
        Mockito.when(mockClient.getUserContext()).thenReturn(user);

        return mockClient;
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
        return newMockBlockEvent(peer, blockNumber, Arrays.asList(transactionEvents));
    }

    public BlockEvent newMockBlockEvent(Peer peer, long blockNumber, Collection<BlockEvent.TransactionEvent> transactionEvents) {
        BlockEvent mockEvent = Mockito.mock(BlockEvent.class);
        Mockito.when(mockEvent.getPeer()).thenReturn(peer);
        Mockito.when(mockEvent.getBlockNumber()).thenReturn(blockNumber);
        Mockito.when(mockEvent.getTransactionEvents()).thenReturn(transactionEvents);
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
        ProposalResponse response = newProposalResponse(200, responsePayload);
        Mockito.when(response.getStatus()).thenReturn(ChaincodeResponse.Status.SUCCESS);
        Mockito.when(response.getProposalResponse()).thenReturn(newFabricProposalResponse());
        return response;
    }

    public ProposalResponse newFailureProposalResponse(String message) {
        ProposalResponse response = newProposalResponse(500, message.getBytes(StandardCharsets.UTF_8));
        Mockito.when(response.getStatus()).thenReturn(ChaincodeResponse.Status.FAILURE);
        Mockito.when(response.getMessage()).thenReturn(message);
        Mockito.when(response.getProposalResponse()).thenReturn(newFabricProposalResponse());
        return response;
    }

    public ProposalResponse newUnavailableProposalResponse(String message) {
        ProposalResponse response = newProposalResponse(500, message.getBytes(StandardCharsets.UTF_8));
        Mockito.when(response.getStatus()).thenReturn(ChaincodeResponse.Status.FAILURE);
        Mockito.when(response.getMessage()).thenReturn(message);
        return response;
    }

    private ProposalResponse newProposalResponse(int statusCode, byte[] responsePayload) {
        ProposalResponse response = Mockito.mock(ProposalResponse.class);
        try {
            Mockito.when(response.getChaincodeActionResponsePayload()).thenReturn(responsePayload);
            Mockito.when(response.getChaincodeActionResponseStatus()).thenReturn(statusCode);
        } catch (InvalidArgumentException e) {
            throw new RuntimeException(e);
        }
        return response;
    }

    private FabricProposalResponse.ProposalResponse newFabricProposalResponse() {
        return FabricProposalResponse.ProposalResponse.getDefaultInstance();
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

    public Consumer<BlockEvent> stubBlockListener() {
        return new Consumer<BlockEvent>() {
            @Override
            public void accept(BlockEvent blockEvent) {}
        };
    }

    public Consumer<BlockEvent.TransactionEvent> stubTransactionListener() {
        return new Consumer<BlockEvent.TransactionEvent>() {
            @Override
            public void accept(BlockEvent.TransactionEvent transactionEvent) {}
        };
    }

    public Consumer<ContractEvent> stubContractListener() {
        return new Consumer<ContractEvent>() {
            @Override
            public void accept(ContractEvent contractEvent) {}
        };
    }

    public Consumer<PeerDisconnectEvent> stubPeerDisconnectListener() {
        return new Consumer<PeerDisconnectEvent>() {
            @Override
            public void accept(PeerDisconnectEvent peerDisconnectEvent) {}
        };
    }

    /**
     * Create a new temporary directory that will be deleted when the JVM exits.
     * @param attributes Attributes to be assigned to the directory.
     * @return The temporary directory.
     * @throws IOException On error.
     */
    public Path createTempDirectory(FileAttribute<?>... attributes) throws IOException {
        Path tempDir = Files.createTempDirectory(TEST_FILE_PREFIX, attributes);
        tempDir.toFile().deleteOnExit();
        return tempDir;
    }

    /**
     * Create a new temporary file within a specific directory that will be deleted when the JVM exits.
     * @param directory A directory in which to create the file.
     * @param attributes Attributes to be assigned to the file.
     * @return The temporary file.
     * @throws IOException On error.
     */
    public Path createTempFile(Path directory, FileAttribute<?>... attributes) throws IOException {
        Path tempFile = Files.createTempFile(directory, TEST_FILE_PREFIX, null, attributes);
        tempFile.toFile().deleteOnExit();
        return tempFile;
    }

    /**
     * Create a new temporary file that will be deleted when the JVM exits.
     * @param attributes Attributes to be assigned to the file.
     * @return The temporary file.
     * @throws IOException On error.
     */
    public Path createTempFile(FileAttribute<?>... attributes) throws IOException {
        Path tempFile = Files.createTempFile(TEST_FILE_PREFIX, null, attributes);
        tempFile.toFile().deleteOnExit();
        return tempFile;
    }

    /**
     * Get a temporary file name within a specific directory that currently does not exist, and mark the file for
     * deletion when the JVM exits.
     * @param directory Parent directory for the file.
     * @return The temporary file.
     * @throws IOException On error.
     */
    public Path getUnusedFilePath(Path directory) throws IOException {
        Path tempFile = Files.createTempFile(directory, UNUSED_FILE_PREFIX, null);
        Files.delete(tempFile);
        tempFile.toFile().deleteOnExit();
        return tempFile;
    }

    /**
     * Get a temporary file name that currently does not exist, and mark the file for deletion when the JVM exits.
     * @return The temporary file.
     * @throws IOException On error.
     */
    public Path getUnusedFilePath() throws IOException {
        Path tempFile = Files.createTempFile(UNUSED_FILE_PREFIX, null);
        Files.delete(tempFile);
        tempFile.toFile().deleteOnExit();
        return tempFile;
    }

    /**
     * Get a temporary directory name that currently does not exist, and mark the directory for deletion when the JVM
     * exits.
     * @return The temporary directory.
     * @throws IOException On error.
     */
    public Path getUnusedDirectoryPath() throws IOException {
        Path tempDir = Files.createTempDirectory(UNUSED_FILE_PREFIX);
        tempDir.toFile().deleteOnExit();
        Files.delete(tempDir);
        return tempDir;
    }

}
