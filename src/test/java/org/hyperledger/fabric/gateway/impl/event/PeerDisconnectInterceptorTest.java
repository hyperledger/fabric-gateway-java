/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.event;

import org.hyperledger.fabric.sdk.Peer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class PeerDisconnectInterceptorTest {
    private Peer peer;
    private Peer.PeerEventingServiceDisconnected originalDisconnectHandler;
    private Peer.PeerEventingServiceDisconnected registeredDisconnectHandler;
    private PeerDisconnectListener listener;
    private Peer.PeerEventingServiceDisconnectEvent sdkEvent;
    private PeerDisconnectEventSource eventSource;

    private void fireEvent(Peer.PeerEventingServiceDisconnectEvent event) {
        registeredDisconnectHandler.disconnected(event);
    }

    @BeforeEach
    public void beforeEach() {
        originalDisconnectHandler = mock(Peer.PeerEventingServiceDisconnected.class);
        registeredDisconnectHandler = originalDisconnectHandler;

        peer = mock(Peer.class);
        when(peer.setPeerEventingServiceDisconnected(any())).thenAnswer(invocation -> {
            Peer.PeerEventingServiceDisconnected currentHandler = registeredDisconnectHandler;
            registeredDisconnectHandler = invocation.getArgument(0);
            return currentHandler;
        });

        listener = mock(PeerDisconnectListener.class);
        sdkEvent = mock(Peer.PeerEventingServiceDisconnectEvent.class);

        eventSource = new PeerDisconnectInterceptor(peer);
        eventSource.addDisconnectListener(listener);
    }

    @Test
    public void listener_receives_event_with_peer() {
        fireEvent(sdkEvent);

        ArgumentCaptor<PeerDisconnectEvent> argument = ArgumentCaptor.forClass(PeerDisconnectEvent.class);
        verify(listener).peerDisconnected(argument.capture());
        assertThat(argument.getValue().getPeer()).isEqualTo(peer);
    }

    @Test
    public void listener_receives_event_with_cause() {
        Throwable expectedCause = new Exception("Peer no like u");
        when(sdkEvent.getExceptionThrown()).thenReturn(expectedCause);

        fireEvent(sdkEvent);

        ArgumentCaptor<PeerDisconnectEvent> argument = ArgumentCaptor.forClass(PeerDisconnectEvent.class);
        verify(listener).peerDisconnected(argument.capture());
        assertThat(argument.getValue().getCause()).isEqualTo(expectedCause);
    }

    @Test
    public void removed_listener_does_not_receive_events() {
        eventSource.removeDisconnectListener(listener);
        fireEvent(sdkEvent);

        verify(listener, never()).peerDisconnected(any());
    }

    @Test
    public void close_restores_original_disconnect_handler() {
        eventSource.close();
        verify(peer).setPeerEventingServiceDisconnected(originalDisconnectHandler);
    }

    @Test
    public void close_removes_listeners() {
        eventSource.close();
        fireEvent(sdkEvent);

        verify(listener, never()).peerDisconnected(any());
    }

    @Test
    public void original_handler_invoked_on_event() {
        fireEvent(sdkEvent);

        verify(originalDisconnectHandler).disconnected(sdkEvent);
    }

    @Test
    public void original_handler_invoked_on_event_if_listener_throws() {
        doThrow(new RuntimeException("fail")).when(listener).peerDisconnected(any());

        fireEvent(sdkEvent);

        verify(originalDisconnectHandler).disconnected(sdkEvent);
    }

    @Test
    public void listener_can_unregister_during_event_handling() {
        PeerDisconnectListener listener = mock(PeerDisconnectListener.class);
        doAnswer(invocation -> {
            eventSource.removeDisconnectListener(listener);
            return null;
        }).when(listener).peerDisconnected(any());
        eventSource.addDisconnectListener(listener);

        fireEvent(sdkEvent);
        verify(listener, times(1)).peerDisconnected(any());

        fireEvent(sdkEvent);
        verify(listener, times(1)).peerDisconnected(any());
    }
}
