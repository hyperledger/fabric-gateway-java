/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway;

import java.util.Optional;

import org.hyperledger.fabric.sdk.BlockEvent;

/**
 * Event emitted by the business logic of a smart contract during execution of a transaction.
 */
public interface ContractEvent {
    /**
     * Get the name of the event emitted by the contract.
     * @return An event name.
     */
    String getName();

    /**
     * Get the identifier of the chaincode that emitted the event.
     * @return A chaincode ID.
     */
    String getChaincodeId();

    /**
     * Get the transaction event that included this contract event.
     * @return The associated transaction event.
     */
    BlockEvent.TransactionEvent getTransactionEvent();

    /**
     * Any binary data associated with this event by the chaincode.
     * @return A binary payload.
     */
    Optional<byte[]> getPayload();
}
