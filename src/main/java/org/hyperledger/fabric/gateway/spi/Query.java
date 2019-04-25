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

public interface Query {
    ProposalResponse evaluate(Peer peer) throws GatewayException;
    Map<Peer, ProposalResponse> evaluate(Collection<Peer> peers) throws GatewayException;
}
