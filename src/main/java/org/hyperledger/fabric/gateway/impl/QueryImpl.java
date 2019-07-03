/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.hyperledger.fabric.gateway.GatewayRuntimeException;
import org.hyperledger.fabric.gateway.spi.Query;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.QueryByChaincodeRequest;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;

public final class QueryImpl implements Query {
    private final Channel channel;
    private final QueryByChaincodeRequest request;

    public QueryImpl(Channel channel, QueryByChaincodeRequest request) {
        this.channel = channel;
        this.request = request;
    }

    @Override
    public ProposalResponse evaluate(Peer peer) {
        try {
            Collection<ProposalResponse> responses = channel.queryByChaincode(request, Collections.singletonList(peer));
            return responses.iterator().next();
        } catch (ProposalException | InvalidArgumentException e) {
            throw new GatewayRuntimeException(e);
        }
    }

    @Override
    public Map<Peer, ProposalResponse> evaluate(Collection<Peer> peers) {
        try {
            Collection<ProposalResponse> responses = channel.queryByChaincode(request, peers);
            return responses.stream()
                    .collect(Collectors.toMap(ProposalResponse::getPeer, response -> response));
        } catch (ProposalException | InvalidArgumentException e) {
            throw new GatewayRuntimeException(e);
        }
    }
}
