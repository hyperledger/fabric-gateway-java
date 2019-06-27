/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.bouncycastle.operator.OperatorCreationException;
import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.ContractEvent;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.GatewayException;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.TestUtils;
import org.hyperledger.fabric.gateway.impl.event.StubBlockEventSource;
import org.hyperledger.fabric.gateway.spi.Checkpointer;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.BlockInfo;
import org.hyperledger.fabric.sdk.ChaincodeEvent;
import org.hyperledger.fabric.sdk.Peer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ContractListenerTest {
    private static final TestUtils testUtils = TestUtils.getInstance();

    private Gateway gateway;
    private StubBlockEventSource blockSource;
    private Contract contract;
    private int transactionNumber = 1;
    private final String chaincodeId = "chaincodeId";
    private final String eventName = "eventName";
    private final Pattern eventNamePattern = Pattern.compile(Pattern.quote(eventName) + ".*");
    private final Consumer<ContractEvent> selfRemovingListener = new Consumer<ContractEvent>() {
        @Override
        public void accept(ContractEvent event) {
            contract.removeContractListener(this);
        }
    };

    @BeforeEach
    public void beforeEach() throws GatewayException, OperatorCreationException, CertificateException, NoSuchAlgorithmException, NoSuchProviderException, IOException {
        blockSource = new StubBlockEventSource(); // Must be created before network
        gateway = testUtils.newGatewayBuilder().connect();
        Network network = gateway.getNetwork("ch1");
        contract = network.getContract(chaincodeId);
    }

    @AfterEach
    public void afterEach() {
        blockSource.close();
        gateway.close();
    }

    private ChaincodeEvent mockChaincodeEvent(String chaincodeId, String name) {
        ChaincodeEvent result = mock(ChaincodeEvent.class);
        when(result.getChaincodeId()).thenReturn(chaincodeId);
        when(result.getEventName()).thenReturn(name);
        return result;
    }

    private ChaincodeEvent mockChaincodeEvent(String chaincodeId, String name, byte[] payload) {
        ChaincodeEvent result = mock(ChaincodeEvent.class);
        when(result.getChaincodeId()).thenReturn(chaincodeId);
        when(result.getEventName()).thenReturn(name);
        when(result.getPayload()).thenReturn(payload);
        return result;
    }

    private void fireEvents(ChaincodeEvent... chaincodeEvents) {
        BlockEvent event = newBlockEvent(1, chaincodeEvents);
        blockSource.sendEvent(event);
    }

    /**
     * Create a block event containing each of the supplied chaincode events attached to a separate transaction within
     * the block.
     * @param blockNumber A block number.
     * @param chaincodeEvents Events to include in the block.
     * @return The block event that was fired.
     */
    private BlockEvent newBlockEvent(long blockNumber, ChaincodeEvent... chaincodeEvents) {
        Peer peer = testUtils.newMockPeer("peer1");
        List<BlockEvent.TransactionEvent> transactionEvents = Arrays.stream(chaincodeEvents)
                .map(chaincodeEvent -> {
                    BlockInfo.TransactionEnvelopeInfo.TransactionActionInfo actionInfo = mock(BlockInfo.TransactionEnvelopeInfo.TransactionActionInfo.class);
                    when(actionInfo.getEvent()).thenReturn(chaincodeEvent);
                    return actionInfo;
                })
                .map(Collections::singletonList)
                .map(actionInfos -> {
                    BlockEvent.TransactionEvent transactionEvent = testUtils.newValidMockTransactionEvent(peer, nextTransactionId());
                    when(transactionEvent.getTransactionActionInfos()).thenReturn(actionInfos);
                    return transactionEvent;
                })
                .collect(Collectors.toList());

        return testUtils.newMockBlockEvent(peer, blockNumber, transactionEvents);
    }

    private String nextTransactionId() {
        return "tx" + transactionNumber++;
    }

    @Test
    public void add_listener_returns_the_listener() {
        Consumer<ContractEvent> listener = event -> {};

        Consumer<ContractEvent> result = contract.addContractListener(listener);

        assertThat(result).isSameAs(listener);
    }

    @Test
    public void add_name_listener_returns_the_listener() {
        Consumer<ContractEvent> listener = event -> {};

        Consumer<ContractEvent> result = contract.addContractListener(listener, eventName);

        assertThat(result).isSameAs(listener);
    }

    @Test
    public void add_pattern_listener_returns_the_listener() {
        Consumer<ContractEvent> listener = event -> {};

        Consumer<ContractEvent> result = contract.addContractListener(listener, eventNamePattern);

        assertThat(result).isSameAs(listener);
    }

    @Test
    public void listener_receives_matching_events() {
        Consumer<ContractEvent> listener = spy(testUtils.stubContractListener());
        ChaincodeEvent event1 = mockChaincodeEvent(chaincodeId, eventName + 1);
        ChaincodeEvent event2 = mockChaincodeEvent(chaincodeId, eventName + 2);

        contract.addContractListener(listener, eventNamePattern);
        fireEvents(event1, event2);

        verify(listener, times(2)).accept(any(ContractEvent.class));
    }

    @Test
    public void listener_without_matcher_receives_all_events() {
        Consumer<ContractEvent> listener = spy(testUtils.stubContractListener());
        ChaincodeEvent event1 = mockChaincodeEvent(chaincodeId, eventName + 1);
        ChaincodeEvent event2 = mockChaincodeEvent(chaincodeId, eventName + 2);

        contract.addContractListener(listener);
        fireEvents(event1, event2);

        verify(listener, times(2)).accept(any(ContractEvent.class));
    }

    @Test
    public void listener_receives_events_with_specific_name() {
        Consumer<ContractEvent> listener = spy(testUtils.stubContractListener());
        ChaincodeEvent event = mockChaincodeEvent(chaincodeId, eventName);

        contract.addContractListener(listener, eventName);
        fireEvents(event);

        verify(listener).accept(any(ContractEvent.class));
    }

    @Test
    public void null_events_are_ignored() {
        Consumer<ContractEvent> listener = spy(testUtils.stubContractListener());
        ChaincodeEvent event = mockChaincodeEvent(chaincodeId, eventName);

        contract.addContractListener(listener);
        fireEvents(event, null);

        verify(listener, times(1)).accept(any(ContractEvent.class));
    }

    @Test
    public void listener_does_not_receive_event_names_not_matching_pattern() {
        Consumer<ContractEvent> listener = spy(testUtils.stubContractListener());
        ChaincodeEvent event = mockChaincodeEvent(chaincodeId, "BAD_" + eventName);

        contract.addContractListener(listener, eventNamePattern);
        fireEvents(event);

        verify(listener, never()).accept(any(ContractEvent.class));
    }

    @Test
    public void listener_does_not_receive_events_without_specific_name() {
        Consumer<ContractEvent> listener = spy(testUtils.stubContractListener());
        ChaincodeEvent event = mockChaincodeEvent(chaincodeId, "BAD_" + eventName);

        contract.addContractListener(listener, eventName);
        fireEvents(event);

        verify(listener, never()).accept(any(ContractEvent.class));
    }

    @Test
    public void listener_does_not_receive_events_from_other_contracts() {
        Consumer<ContractEvent> listener = spy(testUtils.stubContractListener());
        ChaincodeEvent event = mockChaincodeEvent(chaincodeId, "BAD_" + eventName);

        contract.addContractListener(listener, eventNamePattern);
        fireEvents(event);

        verify(listener, never()).accept(any(ContractEvent.class));
    }

    @Test
    public void removed_listener_does_not_receive_events() {
        Consumer<ContractEvent> listener = spy(testUtils.stubContractListener());
        ChaincodeEvent event = mockChaincodeEvent(chaincodeId, eventName);

        contract.addContractListener(listener, eventNamePattern);
        contract.removeContractListener(listener);
        fireEvents(event);

        verify(listener, never()).accept(any(ContractEvent.class));
    }

    @Test
    public void listener_can_unregister_during_event_handling() {
        Consumer<ContractEvent> listener = spy(selfRemovingListener);
        ChaincodeEvent event = mockChaincodeEvent(chaincodeId, eventName);

        contract.addContractListener(listener);
        fireEvents(event);
        fireEvents(event);
        verify(listener, times(1)).accept(any(ContractEvent.class));
    }

    @Test
    public void add_checkpoint_listener_returns_the_listener() throws IOException, GatewayException {
        Consumer<ContractEvent> listener = event -> {};
        Checkpointer checkpointer = new InMemoryCheckpointer();

        Consumer<ContractEvent> result = contract.addContractListener(checkpointer, listener);

        assertThat(result).isSameAs(listener);
    }

    @Test
    public void add_checkpoint_listener_with_event_name_returns_the_listener() throws IOException, GatewayException {
        Consumer<ContractEvent> listener = event -> {};
        Checkpointer checkpointer = new InMemoryCheckpointer();

        Consumer<ContractEvent> result = contract.addContractListener(checkpointer, listener, eventName);

        assertThat(result).isSameAs(listener);
    }

    @Test
    public void add_checkpoint_listener_with_event_pattern_returns_the_listener() throws IOException, GatewayException {
        Consumer<ContractEvent> listener = event -> {};
        Checkpointer checkpointer = new InMemoryCheckpointer();

        Consumer<ContractEvent> result = contract.addContractListener(checkpointer, listener, eventNamePattern);

        assertThat(result).isSameAs(listener);
    }

    @Test
    public void listener_with_new_checkpointer_receives_all_events() throws IOException, GatewayException {
        Consumer<ContractEvent> listener = spy(testUtils.stubContractListener());
        Checkpointer checkpointer = new InMemoryCheckpointer();
        ChaincodeEvent chaincodeEvent1 = mockChaincodeEvent(chaincodeId, eventName + 1);
        ChaincodeEvent chaincodeEvent2 = mockChaincodeEvent(chaincodeId, eventName + 2);
        BlockEvent blockEvent = newBlockEvent(1, chaincodeEvent1, chaincodeEvent2);

        contract.addContractListener(checkpointer, listener);
        blockSource.sendEvent(blockEvent);

        verify(listener, times(2)).accept(any(ContractEvent.class));
    }

    @Test
    public void listener_with_saved_checkpointer_resumes_from_previous_event() throws IOException, GatewayException {
        Consumer<ContractEvent> realtimeListener = event -> {};
        Consumer<ContractEvent> replayListener = spy(testUtils.stubContractListener());
        Checkpointer checkpointer = new InMemoryCheckpointer();
        ChaincodeEvent chaincodeEvent = mockChaincodeEvent(chaincodeId, eventName);
        BlockEvent blockEvent1 = newBlockEvent(1, chaincodeEvent);
        BlockEvent blockEvent2 = newBlockEvent(2, chaincodeEvent);

        contract.addContractListener(checkpointer, realtimeListener);
        blockSource.sendEvent(blockEvent1);
        contract.removeContractListener(realtimeListener);
        blockSource.sendEvent(blockEvent2);
        contract.addContractListener(checkpointer, replayListener);
        blockSource.sendEvent(blockEvent2); // Normally would be ignored as a duplicate block

        verify(replayListener, times(1)).accept(any(ContractEvent.class));
    }

    @Test
    public void listener_with_new_checkpointer_only_receives_events_with_specific_name() throws IOException, GatewayException {
        Consumer<ContractEvent> listener = spy(testUtils.stubContractListener());
        Checkpointer checkpointer = new InMemoryCheckpointer();
        ChaincodeEvent badEvent = mockChaincodeEvent(chaincodeId, "BAD_" + eventName);
        ChaincodeEvent goodEvent = mockChaincodeEvent(chaincodeId, eventName);
        BlockEvent blockEvent = newBlockEvent(1, badEvent, goodEvent);

        contract.addContractListener(checkpointer, listener, eventName);
        blockSource.sendEvent(blockEvent);

        verify(listener, times(1)).accept(any(ContractEvent.class));
    }

    @Test
    public void listener_with_new_checkpointer_only_receives_events_matching_a_pattern() throws IOException, GatewayException {
        Consumer<ContractEvent> listener = spy(testUtils.stubContractListener());
        Checkpointer checkpointer = new InMemoryCheckpointer();
        ChaincodeEvent badEvent = mockChaincodeEvent(chaincodeId, "BAD_" + eventName);
        ChaincodeEvent goodEvent1 = mockChaincodeEvent(chaincodeId, eventName + 1);
        ChaincodeEvent goodEvent2 = mockChaincodeEvent(chaincodeId, eventName + 2);
        BlockEvent blockEvent = newBlockEvent(1, badEvent, goodEvent1, goodEvent2);

        contract.addContractListener(checkpointer, listener, eventNamePattern);
        blockSource.sendEvent(blockEvent);

        verify(listener, times(2)).accept(any(ContractEvent.class));
    }

    @Test
    public void add_replay_listener_returns_the_listener() throws IOException, GatewayException {
        Consumer<ContractEvent> listener = event -> {};

        Consumer<ContractEvent> result = contract.addContractListener(1, listener);

        assertThat(result).isSameAs(listener);
    }

    @Test
    public void add_replay_listener_with_event_name_returns_the_listener() throws IOException, GatewayException {
        Consumer<ContractEvent> listener = event -> {};

        Consumer<ContractEvent> result = contract.addContractListener(1, listener, eventName);

        assertThat(result).isSameAs(listener);
    }

    @Test
    public void add_replay_listener_with_event_pattern_returns_the_listener() throws IOException, GatewayException {
        Consumer<ContractEvent> listener = event -> {};

        Consumer<ContractEvent> result = contract.addContractListener(1, listener, eventNamePattern);

        assertThat(result).isSameAs(listener);
    }

    @Test
    public void replay_listener_receives_events_from_start_block() throws IOException, GatewayException {
        Consumer<ContractEvent> replayListener = spy(testUtils.stubContractListener());
        ChaincodeEvent chaincodeEvent = mockChaincodeEvent(chaincodeId, eventName);
        BlockEvent blockEvent1 = newBlockEvent(1, chaincodeEvent);
        BlockEvent blockEvent2 = newBlockEvent(2, chaincodeEvent);

        contract.addContractListener(testUtils.stubContractListener());
        blockSource.sendEvent(blockEvent2); // Non-replay listeners will now only receive later blocks
        contract.addContractListener(2, replayListener);
        blockSource.sendEvent(blockEvent1); // Should be ignored
        blockSource.sendEvent(blockEvent2); // Should be received

        verify(replayListener, times(1)).accept(any(ContractEvent.class));
    }

    @Test
    public void replay_listener_with_event_name_receives_events_from_start_block() throws IOException, GatewayException {
        Consumer<ContractEvent> replayListener = spy(testUtils.stubContractListener());
        ChaincodeEvent goodEvent = mockChaincodeEvent(chaincodeId, eventName);
        ChaincodeEvent badEvent = mockChaincodeEvent(chaincodeId, "BAD_" + eventName);
        BlockEvent blockEvent1 = newBlockEvent(1, goodEvent);
        BlockEvent blockEvent2 = newBlockEvent(2, goodEvent);
        BlockEvent blockEvent3 = newBlockEvent(3, badEvent);

        contract.addContractListener(testUtils.stubContractListener());
        blockSource.sendEvent(blockEvent2); // Non-replay listeners will now only receive later blocks
        contract.addContractListener(2, replayListener, eventName);
        blockSource.sendEvent(blockEvent1); // Should be ignored
        blockSource.sendEvent(blockEvent2); // Should be received
        blockSource.sendEvent(blockEvent3); // Should be ignored

        verify(replayListener, times(1)).accept(any(ContractEvent.class));
    }

    @Test
    public void replay_listener_with_event_pattern_receives_events_from_start_block() throws IOException, GatewayException {
        Consumer<ContractEvent> replayListener = spy(testUtils.stubContractListener());
        ChaincodeEvent goodEvent = mockChaincodeEvent(chaincodeId, eventName);
        ChaincodeEvent badEvent = mockChaincodeEvent(chaincodeId, "BAD_" + eventName);
        BlockEvent blockEvent1 = newBlockEvent(1, goodEvent);
        BlockEvent blockEvent2 = newBlockEvent(2, goodEvent);
        BlockEvent blockEvent3 = newBlockEvent(3, badEvent);

        contract.addContractListener(testUtils.stubContractListener());
        blockSource.sendEvent(blockEvent2); // Non-replay listeners will now only receive later blocks
        contract.addContractListener(2, replayListener, eventNamePattern);
        blockSource.sendEvent(blockEvent1); // Should be ignored
        blockSource.sendEvent(blockEvent2); // Should be received
        blockSource.sendEvent(blockEvent3); // Should be ignored

        verify(replayListener, times(1)).accept(any(ContractEvent.class));
    }

    @Test
    public void close_contract_removes_listeners() {
        Consumer<ContractEvent> listener = spy(testUtils.stubContractListener());
        ChaincodeEvent event = mockChaincodeEvent(chaincodeId, eventName);

        contract.addContractListener(listener);
        ((ContractImpl)contract).close();
        fireEvents(event);

        verify(listener, never()).accept(any(ContractEvent.class));
    }
}