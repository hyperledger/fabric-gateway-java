/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.event;

import org.hyperledger.fabric.gateway.ContractEvent;

import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Stub implementation of a BlockEventSource to allow tests to drive events into the system.
 *
 * <p>Creating an instance of this class modifies the behaviour of the {@link ContractEventSourceFactory} so that this
 * instance is always returned by the factory. It is important to call {@link #close()} to restore default factory
 * behaviour.</p>
 */
public class StubContractEventSource implements ContractEventSource {
    private final ListenerSet<Consumer<ContractEvent>> listeners = new ListenerSet<>();

    public StubContractEventSource() {
        ContractEventSourceFactory.setFactoryFunction(channel -> this);
    }

    @Override
    public Consumer<ContractEvent> addContractListener(Consumer<ContractEvent> listener, Pattern chaincodeId, Pattern eventName) {
        return listeners.add(listener);
    }

    @Override
    public void removeContractListener(Consumer<ContractEvent> listener) {
        listeners.remove(listener);
    }

    @Override
    public void close() {
        listeners.clear();
        ContractEventSourceFactory.setFactoryFunction(ContractEventSourceFactory.DEFAULT_FACTORY_FN);
    }

    public void sendEvent(ContractEvent event) {
        listeners.forEach(listener -> listener.accept(event));
    }
}
