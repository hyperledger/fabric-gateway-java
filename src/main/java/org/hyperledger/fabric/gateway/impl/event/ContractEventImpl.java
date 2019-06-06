/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.event;

import org.hyperledger.fabric.gateway.ContractEvent;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.ChaincodeEvent;

import java.util.Optional;

/**
 * Provides a contract event view of a chaincode event and associated data.
 */
public final class ContractEventImpl implements ContractEvent {
    private final ChaincodeEvent chaincodeEvent;
    private final BlockEvent.TransactionEvent transactionEvent;

    public ContractEventImpl(BlockEvent.TransactionEvent transactionEvent, ChaincodeEvent chaincodeEvent) {
        this.chaincodeEvent = chaincodeEvent;
        this.transactionEvent = transactionEvent;
    }

    @Override
    public String getName() {
        return chaincodeEvent.getEventName();
    }

    @Override
    public String getChaincodeId() {
        return chaincodeEvent.getChaincodeId();
    }

    @Override
    public BlockEvent.TransactionEvent getTransactionEvent() {
        return transactionEvent;
    }

    @Override
    public Optional<byte[]> getPayload() {
        return Optional.ofNullable(chaincodeEvent.getPayload());
    }
}
