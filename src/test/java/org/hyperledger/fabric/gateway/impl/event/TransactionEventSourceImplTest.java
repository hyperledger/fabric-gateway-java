/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.event;

import org.hyperledger.fabric.gateway.TestUtils;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.Peer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.mockito.Mockito.*;

public class TransactionEventSourceImplTest {
    private static final TestUtils testUtils = TestUtils.getInstance();

    private StubBlockEventSource blockEventSource;
    private TransactionEventSource transactionEventSource;
    private BlockEvent.TransactionEvent transactionEvent;
    private BlockEvent blockEvent;

    @BeforeEach
    public void beforeEach() {
        blockEventSource = new StubBlockEventSource();
        transactionEventSource = new TransactionEventSourceImpl(blockEventSource);

        Peer peer = mock(Peer.class);
        transactionEvent = testUtils.newValidMockTransactionEvent(peer, "transactionId");
        blockEvent = testUtils.newMockBlockEvent(peer, 1, transactionEvent);
    }

    @AfterEach
    public void afterEach() {
        blockEventSource.close();
    }

    private void fireTransactionEvent() {
        blockEventSource.sendEvent(blockEvent);
    }

    @Test
    public void registered_listener_receives_transaction_events() {
        Consumer<BlockEvent.TransactionEvent> listener = spy(testUtils.stubTransactionListener());

        transactionEventSource.addTransactionListener(listener);
        fireTransactionEvent();

        verify(listener).accept(transactionEvent);
    }

    @Test
    public void unregistered_lister_does_not_receive_events() {
        Consumer<BlockEvent.TransactionEvent> listener = spy(testUtils.stubTransactionListener());

        transactionEventSource.addTransactionListener(listener);
        transactionEventSource.removeTransactionListener(listener);
        fireTransactionEvent();

        verify(listener, never()).accept(transactionEvent);
    }

    @Test
    public void close_removes_listeners() {
        Consumer<BlockEvent.TransactionEvent> listener = spy(testUtils.stubTransactionListener());

        transactionEventSource.addTransactionListener(listener);
        transactionEventSource.close();
        fireTransactionEvent();

        verify(listener, never()).accept(transactionEvent);
    }

    @Test
    public void listener_can_unregister_during_event_handling() {
        Consumer<BlockEvent.TransactionEvent> listener = spy(new Consumer<BlockEvent.TransactionEvent>() {
            @Override
            public void accept(BlockEvent.TransactionEvent transactionEvent) {
                transactionEventSource.removeTransactionListener(this);
            }
        });
        transactionEventSource.addTransactionListener(listener);

        fireTransactionEvent();
        verify(listener, times(1)).accept(transactionEvent);

        fireTransactionEvent();
        verify(listener, times(1)).accept(transactionEvent);
    }
}