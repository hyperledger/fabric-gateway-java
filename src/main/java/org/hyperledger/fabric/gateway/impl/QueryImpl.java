/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import org.hyperledger.fabric.gateway.GatewayException;
import org.hyperledger.fabric.gateway.spi.Query;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.QueryByChaincodeRequest;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class QueryImpl implements Query {
    private final Channel channel;
    private final QueryByChaincodeRequest request;

    public QueryImpl(Channel channel, QueryByChaincodeRequest request) {
        this.channel = channel;
        this.request = request;
    }

    @Override
    public ProposalResponse evaluate(Peer peer) throws GatewayException {
        try {
            Collection<ProposalResponse> responses = channel.queryByChaincode(request, Arrays.asList(peer));
            return responses.iterator().next();
        } catch (ProposalException | InvalidArgumentException e) {
            throw new GatewayException(e);
        }
    }

    @Override
    public Map<Peer, ProposalResponse> evaluate(Collection<Peer> peers) throws GatewayException {
        try {
            Collection<ProposalResponse> responses = channel.queryByChaincode(request, peers);
            Map<Peer, ProposalResponse> results = responses.stream()
                    .collect(Collectors.toMap(response -> response.getPeer(), Function.identity()));
            return results;
        } catch (ProposalException | InvalidArgumentException e) {
            throw new GatewayException(e);
        }
    }
}
