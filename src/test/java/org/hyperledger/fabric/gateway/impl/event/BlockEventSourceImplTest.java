/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.event;

import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class BlockEventSourceImplTest {
    private Channel channel;
    private BlockEventSourceImpl blockEventSource;
    private final HashMap<String, org.hyperledger.fabric.sdk.BlockListener> channelListenerMap = new HashMap<>();

    @BeforeEach
    public void beforeEach() throws Exception {
        channel = mock(Channel.class);
        when(channel.registerBlockListener(any())).thenAnswer(invocation -> {
            org.hyperledger.fabric.sdk.BlockListener channelListener = invocation.getArgument(0);
            String handle = String.valueOf(channelListener.hashCode());
            channelListenerMap.put(handle, channelListener);
            return handle;
        });
        when(channel.unregisterBlockListener(any())).thenAnswer(invocation -> {
            return channelListenerMap.remove(invocation.getArgument(0)) != null;
        });

        blockEventSource = new BlockEventSourceImpl(channel);
    }

    @AfterEach
    public void afterEach() {
        channelListenerMap.clear();
    }

    private void fireBlockEvent(BlockEvent event) {
        List<org.hyperledger.fabric.sdk.BlockListener> listeners = new ArrayList<>(channelListenerMap.values());
        listeners.forEach(channelListener -> channelListener.received(event));
    }

    @Test
    public void add_listener_registers_with_channel() throws Exception {
        blockEventSource.addBlockListener(blockEvent -> {});
        verify(channel).registerBlockListener(any());
    }

    @Test
    public void remove_listener_unregisters_with_channel() throws Exception {
        BlockListener listener = blockEventSource.addBlockListener(blockEvent -> {});
        blockEventSource.removeBlockListener(listener);

        verify(channel).unregisterBlockListener(any());
    }

    @org.junit.jupiter.api.Test
    public void add_duplicate_listener_does_not_register_with_channel() throws Exception {
        BlockListener listener = blockEventSource.addBlockListener(blockEvent -> {});
        verify(channel, times(1)).registerBlockListener(any());

        blockEventSource.addBlockListener(listener);
        verify(channel, times(1)).registerBlockListener(any());
    }

    @Test
    public void remove_listener_that_was_not_added_does_not_unregister_with_channel() throws Exception {
        blockEventSource.removeBlockListener(blockEvent -> {});

        verify(channel, never()).registerBlockListener(any());
    }

    @Test
    public void throws_unchecked_if_channel_register_throws() throws Exception {
        reset(channel);
        when(channel.registerBlockListener(any())).thenThrow(InvalidArgumentException.class);

        assertThrows(IllegalStateException.class, () -> blockEventSource.addBlockListener(blockEvent -> {}));
    }

    @Test
    public void does_not_throw_if_channel_unregister_throws() throws Exception {
        reset(channel);
        when(channel.unregisterBlockListener(any())).thenThrow(InvalidArgumentException.class);

        blockEventSource.addBlockListener(blockEvent -> {});
    }

    @Test
    public void forwards_channel_block_events_to_listener() throws Exception {
        BlockListener listener = mock(BlockListener.class);
        BlockEvent blockEvent = mock(BlockEvent.class);

        blockEventSource.addBlockListener(listener);
        fireBlockEvent(blockEvent);

        verify(listener).receivedBlock(blockEvent);
    }

    @Test
    public void close_unregisters_listener_with_channel() throws Exception {
        blockEventSource.addBlockListener(blockEvent -> {});
        blockEventSource.close();

        verify(channel).unregisterBlockListener(any());
    }

    @org.junit.jupiter.api.Test
    public void listener_can_unregister_during_event_handling() {
        BlockListener listener = mock(BlockListener.class);
        doAnswer(invocation -> {
            blockEventSource.removeBlockListener(listener);
            return null;
        }).when(listener).receivedBlock(any());
        blockEventSource.addBlockListener(listener);

        BlockEvent blockEvent = mock(BlockEvent.class);

        fireBlockEvent(blockEvent);
        verify(listener, times(1)).receivedBlock(any());

        fireBlockEvent(blockEvent);
        verify(listener, times(1)).receivedBlock(any());
    }
}
