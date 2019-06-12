/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.event;

import org.hyperledger.fabric.gateway.TestUtils;
import org.hyperledger.fabric.gateway.spi.PeerDisconnectEvent;
import org.hyperledger.fabric.sdk.Peer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class PeerDisconnectInterceptorTest {
    private static TestUtils testUtils = TestUtils.getInstance();

    private Peer peer;
    private Peer.PeerEventingServiceDisconnected originalDisconnectHandler;
    private Consumer<PeerDisconnectEvent> listener;
    private Peer.PeerEventingServiceDisconnectEvent sdkEvent;
    private PeerDisconnectEventSource eventSource;

    private void fireEvent(Peer.PeerEventingServiceDisconnectEvent event) {
        peer.getPeerEventingServiceDisconnected().disconnected(event);
    }

    @BeforeEach
    public void beforeEach() {
        peer = TestUtils.getInstance().newMockPeer("mockPeer");
        originalDisconnectHandler = peer.getPeerEventingServiceDisconnected();

        listener = spy(testUtils.stubPeerDisconnectListener());
        sdkEvent = mock(Peer.PeerEventingServiceDisconnectEvent.class);

        eventSource = new PeerDisconnectInterceptor(peer);
        eventSource.addDisconnectListener(listener);
    }

    @Test
    public void listener_receives_event_with_peer() {
        fireEvent(sdkEvent);

        ArgumentCaptor<PeerDisconnectEvent> argument = ArgumentCaptor.forClass(PeerDisconnectEvent.class);
        verify(listener).accept(argument.capture());
        assertThat(argument.getValue().getPeer()).isEqualTo(peer);
    }

    @Test
    public void listener_receives_event_with_cause() {
        Throwable expectedCause = new Exception("Peer no like u");
        when(sdkEvent.getExceptionThrown()).thenReturn(expectedCause);

        fireEvent(sdkEvent);

        ArgumentCaptor<PeerDisconnectEvent> argument = ArgumentCaptor.forClass(PeerDisconnectEvent.class);
        verify(listener).accept(argument.capture());
        assertThat(argument.getValue().getCause()).isEqualTo(expectedCause);
    }

    @Test
    public void removed_listener_does_not_receive_events() {
        eventSource.removeDisconnectListener(listener);
        fireEvent(sdkEvent);

        verify(listener, never()).accept(any());
    }

    @Test
    public void close_restores_original_disconnect_handler() {
        eventSource.close();
        assertThat(peer.getPeerEventingServiceDisconnected()).isSameAs(originalDisconnectHandler);
    }

    @Test
    public void close_removes_listeners() {
        eventSource.close();
        fireEvent(sdkEvent);

        verify(listener, never()).accept(any());
    }

    @Test
    public void original_handler_invoked_on_event() {
        fireEvent(sdkEvent);

        verify(originalDisconnectHandler).disconnected(sdkEvent);
    }

    @Test
    public void original_handler_invoked_on_event_if_listener_throws() {
        doThrow(new RuntimeException("fail")).when(listener).accept(any());

        fireEvent(sdkEvent);

        verify(originalDisconnectHandler).disconnected(sdkEvent);
    }

    @Test
    public void listener_can_unregister_during_event_handling() {
        Consumer<PeerDisconnectEvent> listener = spy(new Consumer<PeerDisconnectEvent>() {
            @Override
            public void accept(PeerDisconnectEvent peerDisconnectEvent) {
                eventSource.removeDisconnectListener(this);
            }
        });
        eventSource.addDisconnectListener(listener);

        fireEvent(sdkEvent);
        verify(listener, times(1)).accept(any());

        fireEvent(sdkEvent);
        verify(listener, times(1)).accept(any());
    }
}
