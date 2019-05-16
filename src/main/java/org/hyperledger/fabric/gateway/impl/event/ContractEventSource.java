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
 * Allows observing received chaincode events.
 */
public interface ContractEventSource extends AutoCloseable {
    Consumer<ContractEvent> addContractListener(Consumer<ContractEvent> listener, Pattern chaincodeId, Pattern eventName);
    void removeContractListener(Consumer<ContractEvent> listener);

    @Override
    void close();
}
