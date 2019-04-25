/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.event;

import org.hyperledger.fabric.sdk.Peer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

public class PeerDisconnectEventSourceFactoryTest {
    private final PeerDisconnectEventSourceFactory factory = PeerDisconnectEventSourceFactory.getInstance();
    private Peer peer;

    @BeforeEach
    public void beforeEach() {
        peer = Mockito.mock(Peer.class);
    }

    @Test
    public void single_event_source_per_channel() {
        PeerDisconnectEventSource first = factory.getPeerDisconnectEventSource(peer);
        PeerDisconnectEventSource second = factory.getPeerDisconnectEventSource(peer);

        assertThat(first).isSameAs(second);
    }

    @org.junit.jupiter.api.Test
    public void can_override_event_source_for_peer() {
        PeerDisconnectEventSource override = Mockito.mock(PeerDisconnectEventSource.class);

        factory.setPeerDisconnectEventSource(peer, override);
        PeerDisconnectEventSource result = factory.getPeerDisconnectEventSource(peer);

        assertThat(result).isSameAs(override);
    }

    @Test
    public void override_closes_existing_event_source() {
        PeerDisconnectEventSource existing = Mockito.mock(PeerDisconnectEventSource.class);
        PeerDisconnectEventSource override = Mockito.mock(PeerDisconnectEventSource.class);

        factory.setPeerDisconnectEventSource(peer, existing);
        factory.setPeerDisconnectEventSource(peer, override);

        Mockito.verify(existing).close();
    }
}
