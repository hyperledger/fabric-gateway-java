/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.spi;

import org.hyperledger.fabric.gateway.GatewayException;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;

import java.util.Collection;
import java.util.Map;

/**
 * Defines a query and provides methods to evaluate the query on specific peers.
 */
public interface Query {
    /**
     * Evaluate the query on a specific peer.
     * @param peer A peer.
     * @return The query result from the peer.
     * @throws GatewayException If no result can be obtained from the peer.
     */
    ProposalResponse evaluate(Peer peer) throws GatewayException;
    Map<Peer, ProposalResponse> evaluate(Collection<Peer> peers) throws GatewayException;
}
