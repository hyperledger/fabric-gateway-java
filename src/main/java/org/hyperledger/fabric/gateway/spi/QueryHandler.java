/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.spi;

import org.hyperledger.fabric.gateway.GatewayException;
import org.hyperledger.fabric.sdk.ProposalResponse;

/**
 * Handler responsible for evaluating a given query against appropriate peers.
 */
public interface QueryHandler {
    /**
     * Called when the result for a given query is required.
     * @param query The query to evaluate.
     * @return The query result.
     * @throws GatewayException If no result can be obtained for the query.
     */
    ProposalResponse evaluate(Query query) throws GatewayException;
}
