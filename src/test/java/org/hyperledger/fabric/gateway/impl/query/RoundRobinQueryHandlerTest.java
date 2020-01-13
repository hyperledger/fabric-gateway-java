/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.query;

import java.util.Arrays;
import java.util.Collection;

import org.hyperledger.fabric.gateway.ContractException;
import org.hyperledger.fabric.gateway.spi.Query;
import org.hyperledger.fabric.gateway.spi.QueryHandler;
import org.hyperledger.fabric.sdk.Peer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RoundRobinQueryHandlerTest extends CommonQueryHandlerTest {
    @Override
    public QueryHandler newQueryHandler(Collection<Peer> peers) {
        return new RoundRobinQueryHandler(peers);
    }

    @Test
    public void different_peers_for_two_successful_queries() throws ContractException {
        Query query = mock(Query.class);
        when(query.evaluate(any(Peer.class))).thenReturn(successfulResponse);

        QueryHandler handler = newQueryHandler(Arrays.asList(peer1, peer2));
        handler.evaluate(query);
        handler.evaluate(query);

        ArgumentCaptor<Peer> argument = ArgumentCaptor.forClass(Peer.class);
        verify(query, times(2)).evaluate(argument.capture());
        Peer arg1 = argument.getAllValues().get(0);
        Peer arg2 = argument.getAllValues().get(1);
        assertThat(arg1).isNotSameAs(arg2);
    }
}
