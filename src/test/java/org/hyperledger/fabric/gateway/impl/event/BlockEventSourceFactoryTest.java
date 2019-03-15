/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.event;

import org.hyperledger.fabric.sdk.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

public class BlockEventSourceFactoryTest {
    private final BlockEventSourceFactory factory = BlockEventSourceFactory.getInstance();
    private Channel channel;

    @BeforeEach
    public void beforeEach() {
        channel = Mockito.mock(Channel.class);
    }

    @Test
    public void new_event_source_each_call() {
        BlockEventSource first = factory.newBlockEventSource(channel);
        BlockEventSource second = factory.newBlockEventSource(channel);

        assertThat(first, not(sameInstance(second)));
    }

    @org.junit.jupiter.api.Test
    public void can_override_factory_function() {
        BlockEventSource expected = Mockito.mock(BlockEventSource.class);

        BlockEventSourceFactory.setFactoryFunction(channel -> expected);
        BlockEventSource result = factory.newBlockEventSource(channel);

        assertThat(result, sameInstance(expected));
    }
}
