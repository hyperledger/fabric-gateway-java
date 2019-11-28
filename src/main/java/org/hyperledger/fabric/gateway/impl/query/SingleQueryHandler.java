/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.hyperledger.fabric.gateway.ContractException;
import org.hyperledger.fabric.gateway.spi.Query;
import org.hyperledger.fabric.gateway.spi.QueryHandler;
import org.hyperledger.fabric.sdk.ChaincodeResponse;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;

public final class SingleQueryHandler implements QueryHandler {
    private final List<Peer> peers;
    private final AtomicInteger currentPeerIndex = new AtomicInteger(0);

    public SingleQueryHandler(final Collection<Peer> peers) {
        if (peers.size() < 1) {
            throw new IllegalArgumentException("No peers provided");
        }

        this.peers = new ArrayList<>(peers);
    }

    @Override
    public ProposalResponse evaluate(final Query query) throws ContractException {
        int startPeerIndex = currentPeerIndex.get();
        Collection<ProposalResponse> failResponses = new ArrayList<>();

        for (int i = 0; i < peers.size(); i++) {
            int peerIndex = (startPeerIndex + i) % peers.size();
            Peer peer = peers.get(peerIndex);
            ProposalResponse response = query.evaluate(peer);
            if (response.getStatus().equals(ChaincodeResponse.Status.SUCCESS)) {
                currentPeerIndex.set(peerIndex);
                return response;
            }
            if (response.getProposalResponse() != null) {
                currentPeerIndex.set(peerIndex);
                throw new ContractException(response.getMessage(), Collections.singletonList(response));
            }
            failResponses.add(response);
        }

        String message = "No successful responses received. Errors: " + failResponses.stream()
                .map(ProposalResponse::getMessage)
                .collect(Collectors.joining("; "));
        throw new ContractException(message, failResponses);
    }
}
