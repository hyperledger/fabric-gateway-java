/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.event;

import org.hyperledger.fabric.gateway.TestUtils;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

public class ChannelBlockEventSourceTest {
    private static final TestUtils testUtils = TestUtils.getInstance();

    private Channel channel;
    private ChannelBlockEventSource blockEventSource;
    private final Map<String, org.hyperledger.fabric.sdk.BlockListener> channelListenerMap = new HashMap<>();

    @BeforeEach
    public void beforeEach() throws Exception {
        channel = mock(Channel.class);
        when(channel.registerBlockListener(any(org.hyperledger.fabric.sdk.BlockListener.class))).thenAnswer(invocation -> {
            org.hyperledger.fabric.sdk.BlockListener channelListener = invocation.getArgument(0);
            String handle = String.valueOf(channelListener.hashCode());
            channelListenerMap.put(handle, channelListener);
            return handle;
        });
        when(channel.unregisterBlockListener(any())).thenAnswer(invocation -> {
            String handle = invocation.getArgument(0);
            return channelListenerMap.remove(handle) != null;
        });

        blockEventSource = new ChannelBlockEventSource(channel);
    }

    @AfterEach
    public void afterEach() {
        channelListenerMap.clear();
    }

    private void fireBlockEvent(BlockEvent event) {
        List<org.hyperledger.fabric.sdk.BlockListener> listeners = new ArrayList<>(channelListenerMap.values());
        listeners.forEach(listener -> listener.received(event));
    }

    @Test
    public void add_listener_registers_with_channel() throws Exception {
        blockEventSource.addBlockListener(blockEvent -> {});
        verify(channel).registerBlockListener(any(org.hyperledger.fabric.sdk.BlockListener.class));
    }

    @Test
    public void remove_listener_unregisters_with_channel() throws Exception {
        Consumer<BlockEvent> listener = blockEventSource.addBlockListener(blockEvent -> {});
        blockEventSource.removeBlockListener(listener);

        verify(channel).unregisterBlockListener(any(String.class));
    }

    @org.junit.jupiter.api.Test
    public void add_duplicate_listener_does_not_register_with_channel() throws Exception {
        Consumer<BlockEvent> listener = blockEventSource.addBlockListener(blockEvent -> {});
        verify(channel, times(1)).registerBlockListener(any(org.hyperledger.fabric.sdk.BlockListener.class));

        blockEventSource.addBlockListener(listener);
        verify(channel, times(1)).registerBlockListener(any(org.hyperledger.fabric.sdk.BlockListener.class));
    }

    @Test
    public void remove_listener_that_was_not_added_does_not_unregister_with_channel() throws Exception {
        blockEventSource.removeBlockListener(blockEvent -> {});

        verify(channel, never()).registerBlockListener(any(org.hyperledger.fabric.sdk.BlockListener.class));
    }

    @Test
    public void throws_unchecked_if_channel_register_throws() throws Exception {
        reset(channel);
        when(channel.registerBlockListener(any(org.hyperledger.fabric.sdk.BlockListener.class))).thenThrow(InvalidArgumentException.class);

        assertThatThrownBy(() -> blockEventSource.addBlockListener(blockEvent -> {}))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void does_not_throw_if_channel_unregister_throws() throws Exception {
        Consumer<BlockEvent> listener = blockEventSource.addBlockListener(blockEvent -> {});

        reset(channel);
        when(channel.unregisterBlockListener(any(String.class))).thenThrow(InvalidArgumentException.class);

        blockEventSource.removeBlockListener(listener);
    }

    @Test
    public void forwards_channel_block_events_to_listener() {
        Consumer<BlockEvent> listener = spy(testUtils.stubBlockListener());
        BlockEvent blockEvent = mock(BlockEvent.class);

        blockEventSource.addBlockListener(listener);
        fireBlockEvent(blockEvent);

        verify(listener).accept(blockEvent);
    }

    @Test
    public void close_unregisters_listener_with_channel() throws Exception {
        blockEventSource.addBlockListener(blockEvent -> {});
        blockEventSource.close();

        verify(channel).unregisterBlockListener(any());
    }

    @org.junit.jupiter.api.Test
    public void listener_can_unregister_during_event_handling() {
        Consumer<BlockEvent> listener = spy(new Consumer<BlockEvent>() {
            @Override
            public void accept(BlockEvent blockEvent) {
                blockEventSource.removeBlockListener(this);
            }
        });
        blockEventSource.addBlockListener(listener);

        BlockEvent blockEvent = mock(BlockEvent.class);

        fireBlockEvent(blockEvent);
        verify(listener, times(1)).accept(any());

        fireBlockEvent(blockEvent);
        verify(listener, times(1)).accept(any());
    }
}
