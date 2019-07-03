/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.spi;

import org.hyperledger.fabric.gateway.ContractException;
import org.hyperledger.fabric.sdk.ProposalResponse;

/**
 * Handler responsible for evaluating a given query against appropriate peers.
 */
public interface QueryHandler {
    /**
     * Called when the result for a given query is required.
     * @param query The query to evaluate.
     * @return The query result.
     * @throws ContractException if no peers are reachable or an error response is returned.
     */
    ProposalResponse evaluate(Query query) throws ContractException;
}
