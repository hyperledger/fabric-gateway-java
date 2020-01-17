/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.hyperledger.fabric.sdk.ProposalResponse;

/**
 * Thrown when an error occurs invoking a smart contract.
 */
public class ContractException extends GatewayException {
    private static final long serialVersionUID = -1278679656087547825L;

    private Collection<ProposalResponse> proposalResponses;

    /**
     * Constructs a new exception with the specified detail message. The cause and payload are not initialized.
     * @param message the detail message.
     */
    public ContractException(final String message) {
        super(message);
        this.proposalResponses = Collections.emptyList();
    }

    /**
     * Constructs a new exception with the specified detail message and cause. The payload is not initialized.
     * @param message the detail message.
     * @param cause the cause.
     */
    public ContractException(final String message, final Throwable cause) {
        super(message, cause);
        this.proposalResponses = Collections.emptyList();
    }

    /**
     * Constructs a new exception with the specified detail message and proposal responses returned from peer
     * invocations. The cause is not initialized.
     * @param message the detail message.
     * @param proposalResponses the proposal responses.
     */
    public ContractException(final String message, final Collection<ProposalResponse> proposalResponses) {
        super(message);
        this.proposalResponses = proposalResponses;
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
