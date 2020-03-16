/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.commit;

import java.util.Arrays;
import java.util.Collection;

import org.hyperledger.fabric.gateway.TestUtils;
import org.hyperledger.fabric.sdk.Peer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AllCommitStrategyTest {
    private final TestUtils testUtils = TestUtils.getInstance();
    private CommitStrategy strategy;
    private Collection<Peer> peers;
    private Peer peer1;
    private Peer peer2;

    @BeforeEach
    public void beforeEach() {
        peer1 = testUtils.newMockPeer("peer1");
        peer2 = testUtils.newMockPeer("peer2");
        peers = Arrays.asList(peer1, peer2);
        strategy = new AllCommitStrategy(peers);
    }

    @Test
    public void returns_configured_peers() {
        assertThat(strategy.getPeers()).isEqualTo(peers);
    }

    @Test
    public void continue_if_one_event_received() {
        CommitStrategy.Result result = strategy.onEvent(testUtils.newValidMockTransactionEvent(peer1, "txId"));

        assertThat(result).isEqualTo(CommitStrategy.Result.CONTINUE);
    }

    @Test
    public void continue_if_one_disconnect_received() {
        CommitStrategy.Result result = strategy.onError(testUtils.newPeerDisconnectedEvent(peer1));

        assertThat(result).isEqualTo(CommitStrategy.Result.CONTINUE);
    }

    @Test
    public void success_if_all_events_received() {
        strategy.onEvent(testUtils.newValidMockTransactionEvent(peer1, "txId"));
        CommitStrategy.Result result = strategy.onEvent(testUtils.newValidMockTransactionEvent(peer2, "txId"));

        assertThat(result).isEqualTo(CommitStrategy.Result.SUCCESS);
    }

    @Test
    public void fail_if_all_disconnects_received() {
        strategy.onError(testUtils.newPeerDisconnectedEvent(peer1));
        CommitStrategy.Result result = strategy.onError(testUtils.newPeerDisconnectedEvent(peer2));

        assertThat(result).isEqualTo(CommitStrategy.Result.FAIL);
    }

    @Test
    public void success_if_one_event_and_one_disconnect_received() {
        strategy.onEvent(testUtils.newValidMockTransactionEvent(peer1, "txId"));
        CommitStrategy.Result result = strategy.onError(testUtils.newPeerDisconnectedEvent(peer2));

        assertThat(result).isEqualTo(CommitStrategy.Result.SUCCESS);
    }
}
