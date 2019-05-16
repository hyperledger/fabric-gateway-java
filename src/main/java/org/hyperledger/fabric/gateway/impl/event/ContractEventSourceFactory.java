/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.event;

import org.hyperledger.fabric.sdk.Channel;

import java.util.function.Function;

/**
 * Factory for creating ContractEventSource instances for channels.
 */
public final class ContractEventSourceFactory {
    /** Exposed only for testing. */
    static final Function<Channel, ContractEventSource> DEFAULT_FACTORY_FN = ChannelContractEventSource::new;

    private static final ContractEventSourceFactory INSTANCE = new ContractEventSourceFactory();
    private static Function<Channel, ContractEventSource> factoryFn = DEFAULT_FACTORY_FN;

    public static ContractEventSourceFactory getInstance() {
        return INSTANCE;
    }

    /**
     * Used only for testing. Sets the factory function for creating event sources.
     * @param newFactoryFn New factory function.
     */
    static void setFactoryFunction(Function<Channel, ContractEventSource> newFactoryFn) {
        factoryFn = newFactoryFn;
    }

    private ContractEventSourceFactory() { }

    /**
     * Create an event source for a given channel. The event source is owned and should be closed by the caller.
     * @return Event source instance.
     */
    public ContractEventSource newContractEventSource(Channel channel) {
        return factoryFn.apply(channel);
    }
}
