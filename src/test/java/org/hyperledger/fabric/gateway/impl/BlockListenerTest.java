/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.GatewayException;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.TestUtils;
import org.hyperledger.fabric.gateway.impl.event.StubBlockEventSource;
import org.hyperledger.fabric.gateway.spi.Checkpointer;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.Peer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

public class BlockListenerTest {
    private static final TestUtils testUtils = TestUtils.getInstance();

    private Network network = null;
    private StubBlockEventSource stubBlockEventSource = null;
    private final Peer peer1 = testUtils.newMockPeer("peer1");
    private final Peer peer2 = testUtils.newMockPeer("peer2");

    @BeforeEach
    public void beforeEach() throws Exception {
        stubBlockEventSource = new StubBlockEventSource(); // Must be before network is created
        Gateway gateway = testUtils.newGatewayBuilder().connect();
        network = gateway.getNetwork("ch1");
    }

    @AfterEach
    public void afterEach() {
        stubBlockEventSource.close();
    }

    @Test
    public void add_listener_returns_the_listener() {
        Consumer<BlockEvent> listener = blockEvent -> {};

        Consumer<BlockEvent> result = network.addBlockListener(listener);

        assertThat(result).isSameAs(listener);
    }

    @Test
    public void listener_receives_events() {
        Consumer<BlockEvent> listener = Mockito.spy(testUtils.stubBlockListener());
        BlockEvent event = testUtils.newMockBlockEvent(peer1, 2);

        network.addBlockListener(listener);
        stubBlockEventSource.sendEvent(event);

        Mockito.verify(listener).accept(event);
    }

    @Test
    public void removed_listener_does_not_receive_events() {
        Consumer<BlockEvent> listener = Mockito.spy(testUtils.stubBlockListener());
        BlockEvent event = testUtils.newMockBlockEvent(peer1, 1);

        network.addBlockListener(listener);
        network.removeBlockListener(listener);
        stubBlockEventSource.sendEvent(event);

        Mockito.verify(listener, Mockito.never()).accept(event);
    }

    @Test
    public void duplicate_events_ignored() {
        Consumer<BlockEvent> listener = Mockito.spy(testUtils.stubBlockListener());
        BlockEvent event = testUtils.newMockBlockEvent(peer1, 1);
        BlockEvent duplicateEvent = testUtils.newMockBlockEvent(peer2, 1);

        network.addBlockListener(listener);
        stubBlockEventSource.sendEvent(event);
        stubBlockEventSource.sendEvent(duplicateEvent);

        Mockito.verify(listener, Mockito.never()).accept(duplicateEvent);
    }

    @Test
    public void listener_receives_events_in_order() {
        Consumer<BlockEvent> listener = Mockito.spy(testUtils.stubBlockListener());
        BlockEvent event1 = testUtils.newMockBlockEvent(peer1, 1);
        BlockEvent event2 = testUtils.newMockBlockEvent(peer1, 2);
        BlockEvent event3 = testUtils.newMockBlockEvent(peer1, 3);

        network.addBlockListener(listener);
        stubBlockEventSource.sendEvent(event1); // Prime the listener with an initial block number
        stubBlockEventSource.sendEvent(event3);
        stubBlockEventSource.sendEvent(event2);

        InOrder orderVerifier = Mockito.inOrder(listener);
        orderVerifier.verify(listener).accept(event1);
        orderVerifier.verify(listener).accept(event2);
        orderVerifier.verify(listener).accept(event3);
    }

    @Test
    public void old_events_ignored() {
        Consumer<BlockEvent> listener = Mockito.spy(testUtils.stubBlockListener());
        BlockEvent event1 = testUtils.newMockBlockEvent(peer1, 1);
        BlockEvent event2 = testUtils.newMockBlockEvent(peer1, 2);

        network.addBlockListener(listener);
        stubBlockEventSource.sendEvent(event2);
        stubBlockEventSource.sendEvent(event1);

        Mockito.verify(listener, Mockito.never()).accept(event1);
    }

    @Test
    public void still_receive_new_events_after_ignoring_old_events() {
        Consumer<BlockEvent> listener = Mockito.spy(testUtils.stubBlockListener());
        BlockEvent event1 = testUtils.newMockBlockEvent(peer1, 1);
        BlockEvent event2 = testUtils.newMockBlockEvent(peer1, 2);
        BlockEvent event3 = testUtils.newMockBlockEvent(peer1, 3);

        network.addBlockListener(listener);
        stubBlockEventSource.sendEvent(event2); // Prime the listener with an initial block number
        stubBlockEventSource.sendEvent(event1);
        stubBlockEventSource.sendEvent(event3);

        Mockito.verify(listener).accept(event3);
    }

    @Test
    public void add_checkpoint_listener_returns_the_listener() throws GatewayException, IOException {
        Consumer<BlockEvent> listener = blockEvent -> {};
        Checkpointer checkpointer = new StubCheckpointer();

        Consumer<BlockEvent> result = network.addBlockListener(listener, checkpointer);

        assertThat(result).isSameAs(listener);
    }

    @Test
    public void listener_with_new_checkpointer_receives_events() throws GatewayException, IOException {
        Consumer<BlockEvent> listener = Mockito.spy(testUtils.stubBlockListener());
        Checkpointer checkpointer = new StubCheckpointer();
        BlockEvent event = testUtils.newMockBlockEvent(peer1, 2);

        network.addBlockListener(listener, checkpointer);
        stubBlockEventSource.sendEvent(event);

        Mockito.verify(listener).accept(event);
    }

    @Test
    public void removed_checkpoint_listener_does_not_receive_events() throws GatewayException, IOException {
        Consumer<BlockEvent> listener = Mockito.spy(testUtils.stubBlockListener());
        Checkpointer checkpointer = new StubCheckpointer();
        BlockEvent event = testUtils.newMockBlockEvent(peer1, 2);

        network.addBlockListener(listener, checkpointer);
        network.removeBlockListener(listener);
        stubBlockEventSource.sendEvent(event);

        Mockito.verify(listener, Mockito.never()).accept(event);
    }

    @Test
    public void listener_with_saved_checkpointer_resumes_from_previous_event() throws GatewayException, IOException {
        Consumer<BlockEvent> listener = Mockito.spy(testUtils.stubBlockListener());
        Checkpointer checkpointer = new StubCheckpointer();
        BlockEvent event1 = testUtils.newMockBlockEvent(peer1, 1);
        BlockEvent event2 = testUtils.newMockBlockEvent(peer1, 2);

        network.addBlockListener(listener, checkpointer);
        stubBlockEventSource.sendEvent(event1);
        network.removeBlockListener(listener);
        stubBlockEventSource.sendEvent(event2);
        network.addBlockListener(listener, checkpointer);
        stubBlockEventSource.sendEvent(event2); // Without checkpoint this will be ignored as a duplicate

        Mockito.verify(listener, Mockito.times(1)).accept(event2);
    }

//    @Test
//    public void add_checkpoint_name_listener_returns_the_listener() throws IOException {
//        Consumer<BlockEvent> listener = blockEvent -> {};
//
//        Consumer<BlockEvent> result = network.addBlockListener(listener, "checkpoint");
//
//        assertThat(result).isSameAs(listener);
//    }

//    @Test
//    public void checkpoint_name_listener_receives_events() throws IOException {
//        Consumer<BlockEvent> listener = Mockito.spy(testUtils.stubBlockListener());
//        BlockEvent event = testUtils.newMockBlockEvent(peer1, 2);
//
//        network.addBlockListener(listener, "checkpoint");
//        stubBlockEventSource.sendEvent(event);
//
//        Mockito.verify(listener).accept(event);
//    }

//    @Test
//    public void removed_checkpoint_listener_does_not_receive_events() throws IOException {
//        Consumer<BlockEvent> listener = Mockito.spy(testUtils.stubBlockListener());
//        BlockEvent event = testUtils.newMockBlockEvent(peer1, 1);
//
//        network.addBlockListener(listener, "checkpoint");
//        network.removeBlockListener(listener);
//        stubBlockEventSource.sendEvent(event);
//
//        Mockito.verify(listener, Mockito.never()).accept(event);
//    }

//    @Test
//    public void checkpoint_name_listener_resumes_from_previous_event() throws IOException {
//        Consumer<BlockEvent> listener = Mockito.spy(testUtils.stubBlockListener());
//        BlockEvent event1 = testUtils.newMockBlockEvent(peer1, 1);
//        BlockEvent event2 = testUtils.newMockBlockEvent(peer1, 2);
//        String checkpointerName = "checkpoint";
//
//        network.addBlockListener(listener, checkpointerName);
//        stubBlockEventSource.sendEvent(event1);
//        network.removeBlockListener(listener);
//        stubBlockEventSource.sendEvent(event2); // Without checkpointing this event has been missed
//        network.addBlockListener(listener, checkpointerName);
//        stubBlockEventSource.sendEvent(event2); // Without checkpoint this will be ignored as a duplicate
//
//        Mockito.verify(listener, Mockito.times(1)).accept(event2);
//    }
}
