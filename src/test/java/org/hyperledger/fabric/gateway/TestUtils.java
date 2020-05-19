/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.hyperledger.fabric.gateway.impl.GatewayImpl;
import org.hyperledger.fabric.gateway.impl.identity.GatewayUser;
import org.hyperledger.fabric.gateway.spi.PeerDisconnectEvent;
import org.hyperledger.fabric.protos.peer.ProposalResponsePackage;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.ChaincodeResponse;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.QueryByChaincodeRequest;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ServiceDiscoveryException;
import org.hyperledger.fabric.sdk.identity.X509Enrollment;
import org.hyperledger.fabric.sdk.transaction.TransactionContext;
import org.mockito.Mockito;

public final class TestUtils {
    private static final TestUtils INSTANCE = new TestUtils();
    private static final String TEST_FILE_PREFIX = "fgj-test-";
    private static final String UNUSED_FILE_PREFIX = "fgj-unused-";
    private static final Path NETWORK_CONFIG_PATH = Paths.get("src", "test", "java", "org", "hyperledger", "fabric", "gateway", "connection.json");

    private final AtomicLong currentTransactionId = new AtomicLong();

    public static TestUtils getInstance() {
        return INSTANCE;
    }

    private TestUtils() { }

    public GatewayImpl.Builder newGatewayBuilder() throws IOException {
        X509Credentials credentials = new X509Credentials();

        GatewayImpl.Builder builder = (GatewayImpl.Builder)Gateway.createBuilder();
        Wallet wallet = Wallets.newInMemoryWallet();
        wallet.put("user", Identities.newX509Identity("msp1", credentials.getCertificate(), credentials.getPrivateKey()));
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

    public HFClient newMockClient() {
        X509Credentials credentials = new X509Credentials();
        Enrollment enrollment = new X509Enrollment(credentials.getPrivateKey(), credentials.getCertificatePem());
        User user = new GatewayUser("user", "msp1", enrollment);

        HFClient mockClient = Mockito.mock(HFClient.class);
        Mockito.when(mockClient.getUserContext()).thenReturn(user);
        Mockito.when(mockClient.newTransactionProposalRequest()).thenReturn(TransactionProposalRequest.newInstance(user));
        Mockito.when(mockClient.newQueryProposalRequest()).thenReturn(QueryByChaincodeRequest.newInstance(user));

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
        Mockito.when(mockChannel.newTransactionContext())
                .thenAnswer(invocation -> newMockTransactionContext());

        AtomicReference<Channel.SDPeerAddition> sdPeerAdditionRef = new AtomicReference<>(newMockSDPeerAddition());
        Mockito.when(mockChannel.getSDPeerAddition())
                .thenAnswer(invocation -> sdPeerAdditionRef.get());
        Mockito.when(mockChannel.setSDPeerAddition(Mockito.any(Channel.SDPeerAddition.class)))
                .thenAnswer(invocation -> sdPeerAdditionRef.getAndSet(invocation.getArgument(0)));

        return mockChannel;
    }

    private TransactionContext newMockTransactionContext() {
        TransactionContext mockContext = Mockito.mock(TransactionContext.class);
        Mockito.when(mockContext.getTxID()).thenReturn(newFakeTransactionId());
        return mockContext;
    }

    private String newFakeTransactionId() {
        return Long.toHexString(currentTransactionId.incrementAndGet());
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

    public ProposalResponse newSuccessfulProposalResponse() {
        return newSuccessfulProposalResponse(new byte[0]);
    }

    public ProposalResponse newSuccessfulProposalResponse(String responsePayload) {
        return newSuccessfulProposalResponse(responsePayload.getBytes());
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

    private ProposalResponsePackage.ProposalResponse newFabricProposalResponse() {
        return ProposalResponsePackage.ProposalResponse.getDefaultInstance();
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

    /**
     * Create a new Reader instance that will fail on any read attempt with the provided exception message.
     * @param failMessage Read exception message.
     * @return A reader.
     */
    public Reader newFailingReader(final String failMessage) {
        return new Reader() {
            @Override
            public int read(char[] cbuf, int offset, int length) throws IOException {
                throw new IOException(failMessage);
            }

            @Override
            public void close() {
            }
        };
    }
}
