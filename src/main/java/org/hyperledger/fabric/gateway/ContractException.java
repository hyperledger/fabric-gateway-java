/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import org.hyperledger.fabric.sdk.ProposalResponse;

/**
 * Thrown when an error occurs invoking a smart contract.
 */
public class ContractException extends GatewayException {
    private final byte[] payload;
    private Collection<ProposalResponse> proposalResponses;

    /**
     * Constructs a new exception with the specified detail message. The cause, payload and proposal responses are
     * not initialized.
     * @param message the detail message.
     */
    public ContractException(final String message) {
        super(message);
        this.payload = null;
        this.proposalResponses = Collections.emptyList();
    }

    /**
     * Constructs a new exception with the specified detail message and cause. The payload and proposal responses are
     * not initialized.
     * @param message the detail message.
     * @param cause the cause.
     */
    public ContractException(final String message, final Throwable cause) {
        super(message, cause);
        this.payload = null;
        this.proposalResponses = Collections.emptyList();
    }

    /**
     * Constructs a new exception with the specified detail message and response payload. The cause and proposal
     * responses are not initialized.
     * @param message the detail message.
     * @param payload the error response payload.
     */
    @Deprecated
    public ContractException(final String message, final byte[] payload) {
        super(message);
        this.payload = payload;
    }

    /**
     * Constructs a new exception with the specified detail message and proposal responses returned from peer
     * invocations. The cause and payload are not initialized.
     * @param message the detail message.
     * @param proposalResponses the proposal responses.
     */
    public ContractException(final String message, final Collection<ProposalResponse> proposalResponses) {
        super(message);
        this.payload = null;
        this.proposalResponses = proposalResponses;
    }

    /**
     * Get the error response payload received from the smart contract.
     * @return the response payload.
     */
    @Deprecated
    public Optional<byte[]> getPayload() {
        return Optional.ofNullable(payload);
    }

    /**
     * Get the proposal responses received from peer invocations.
     * @return the proposal responses.
     */
    public Collection<ProposalResponse> getProposalResponses() {
        return new ArrayList<>(proposalResponses);
    }

    /**
     * Set the proposal responses received from peer invocations.
     * @param proposalResponses the proposal responses.
     */
    public void setProposalResponses(final Collection<ProposalResponse> proposalResponses) {
        this.proposalResponses = proposalResponses;
    }
}
