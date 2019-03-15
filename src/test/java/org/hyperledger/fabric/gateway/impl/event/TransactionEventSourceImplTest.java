/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.event;

import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.Peer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.mockito.Mockito.*;

public class TransactionEventSourceImplTest {
    private StubBlockEventSource blockEventSource;
    private TransactionEventSource transactionEventSource;
    private BlockEvent.TransactionEvent transactionEvent;

    @BeforeEach
    public void beforeEach() {
        blockEventSource = new StubBlockEventSource();
        transactionEventSource = new TransactionEventSourceImpl(blockEventSource);

        Peer peer = mock(Peer.class);

        transactionEvent = mock(BlockEvent.TransactionEvent.class);
        doReturn(peer).when(transactionEvent).getPeer();
    }

    @AfterEach
    public void afterEach() {
        blockEventSource.close();
    }

    private void fireTransactionEvent(BlockEvent.TransactionEvent transactionEvent) {
        BlockEvent blockEvent = mock(BlockEvent.class);
        doReturn(transactionEvent.getPeer()).when(blockEvent).getPeer();
        doReturn(Arrays.asList(transactionEvent)).when(blockEvent).getTransactionEvents();

        fireBlockEvent(blockEvent);
    }
    private void fireBlockEvent(BlockEvent event) {
        blockEventSource.sendEvent(event);
    }

    @Test
    public void registered_listener_receives_transaction_events() {
        TransactionListener listener = mock(TransactionListener.class);

        transactionEventSource.addTransactionListener(listener);
        fireTransactionEvent(transactionEvent);

        verify(listener).receivedTransaction(transactionEvent);
    }

    @Test
    public void unregistered_lister_does_not_receive_events() {
        TransactionListener listener = mock(TransactionListener.class);

        transactionEventSource.addTransactionListener(listener);
        transactionEventSource.removeTransactionListener(listener);
        fireTransactionEvent(transactionEvent);

        verify(listener, never()).receivedTransaction(transactionEvent);
    }

    @Test
    public void close_removes_listeners() {
        TransactionListener listener = mock(TransactionListener.class);

        transactionEventSource.addTransactionListener(listener);
        transactionEventSource.close();
        fireTransactionEvent(transactionEvent);

        verify(listener, never()).receivedTransaction(transactionEvent);
    }

    @Test
    public void listener_can_unregister_during_event_handling() {
        TransactionListener listener = mock(TransactionListener.class);
        doAnswer(invocation -> {
            transactionEventSource.removeTransactionListener(listener);
            return null;
        }).when(listener).receivedTransaction(any());
        transactionEventSource.addTransactionListener(listener);

        fireTransactionEvent(transactionEvent);
        verify(listener, times(1)).receivedTransaction(any());

        fireTransactionEvent(transactionEvent);
        verify(listener, times(1)).receivedTransaction(any());
    }
}