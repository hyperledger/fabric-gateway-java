/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.spi;

import java.util.Collection;
import java.util.Map;

import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;

/**
 * Defines a query and provides methods to evaluate the query on specific peers.
 */
public interface Query {
    /**
     * Evaluate the query on a specific peer.
     * @param peer A peer.
     * @return The query result from the peer.
     * @throws org.hyperledger.fabric.gateway.GatewayRuntimeException if misconfiguration of system state prevents
     * queries from being sent.
     */
    ProposalResponse evaluate(Peer peer);

    /**
     * Evaluate the query on several peers.
     * @param peers Peers to query.
     * @return The query results from the peers.
     * @throws org.hyperledger.fabric.gateway.GatewayRuntimeException if misconfiguration of system state prevents
     * queries from being sent.
     */
    Map<Peer, ProposalResponse> evaluate(Collection<Peer> peers);
}
