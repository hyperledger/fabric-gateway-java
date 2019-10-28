/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.event;

import java.util.function.Function;

import org.hyperledger.fabric.sdk.Channel;

/**
 * Factory for creating BlockEventSource instances for channels.
 */
public final class BlockEventSourceFactory {
    /** Exposed only for testing. */
    static final Function<Channel, BlockEventSource> DEFAULT_FACTORY_FN = ChannelBlockEventSource::new;

    private static final BlockEventSourceFactory INSTANCE = new BlockEventSourceFactory();
    private static Function<Channel, BlockEventSource> factoryFn = DEFAULT_FACTORY_FN;

    public static BlockEventSourceFactory getInstance() {
        return INSTANCE;
    }

    /**
     * Used only for testing. Sets the factory function for creating event sources.
     * @param newFactoryFn New factory function.
     */
    static void setFactoryFunction(final Function<Channel, BlockEventSource> newFactoryFn) {
        factoryFn = newFactoryFn;
    }

    private BlockEventSourceFactory() { }

    /**
     * Create an event source for a given channel. The event source is owned and should be closed by the caller.
     * @param channel Channel for which an event source should be created.
     * @return Event source instance.
     */
    public BlockEventSource newBlockEventSource(final Channel channel) {
        return factoryFn.apply(channel);
    }
}
