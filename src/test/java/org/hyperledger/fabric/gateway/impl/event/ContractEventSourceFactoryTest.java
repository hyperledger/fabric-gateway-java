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

import static org.assertj.core.api.Assertions.assertThat;

public class ContractEventSourceFactoryTest {
    private final ContractEventSourceFactory factory = ContractEventSourceFactory.getInstance();
    private Channel channel;

    @BeforeEach
    public void beforeEach() {
        channel = Mockito.mock(Channel.class);
    }

    @Test
    public void new_event_source_each_call() {
        ContractEventSource first = factory.newContractEventSource(channel);
        ContractEventSource second = factory.newContractEventSource(channel);

        assertThat(first).isNotSameAs(second);
    }

    @Test
    public void can_override_factory_function() {
        ContractEventSource expected = Mockito.mock(ContractEventSource.class);

        ContractEventSourceFactory.setFactoryFunction(channel -> expected);
        ContractEventSource result = factory.newContractEventSource(channel);

        assertThat(result).isSameAs(expected);
    }
}
