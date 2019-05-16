/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway;

import org.hyperledger.fabric.sdk.BlockEvent;

import java.util.Optional;

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
     * Get the identifier of the transaction invocation that emitted this event.
     * @return A transaction ID.
     */
    String getTransactionId();

    /**
     * Any binary data associated with this event by the chaincode.
     * @return A binary payload.
     */
    Optional<byte[]> getPayload();

    /**
     * Get the block that contained this event.
     * @return A block event.
     */
    BlockEvent getBlockEvent();
}
