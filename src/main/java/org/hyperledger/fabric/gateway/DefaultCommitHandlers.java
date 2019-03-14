/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway;

import org.hyperledger.fabric.gateway.impl.NoOpCommitHandler;
import org.hyperledger.fabric.gateway.spi.CommitHandler;
import org.hyperledger.fabric.gateway.spi.CommitHandlerFactory;
import org.hyperledger.fabric.sdk.Peer;

import java.util.Collection;
import java.util.Collections;

public final class DefaultCommitHandlers {
    public static CommitHandlerFactory NONE = new CommitHandlerFactory() {
        @Override
        public CommitHandler create(String transactionId, Network network) {
            return NoOpCommitHandler.INSTANCE;
        }
    };

    public static CommitHandlerFactory MSPID_SCOPE_ALLFORTX = new CommitHandlerFactory() {
        @Override
        public CommitHandler create(String transactionId, Network network) {
            Collection<Peer> peers = Collections.emptyList();
//            Collection<TxEventListener> listeners = Collections.emptyList(); // Listeners for each peer, and which know the tx ID to listen for
            Object strategy = null; // TODO
            CommitHandler handler = null; //new CommitHandlerImpl(strategy);
            return handler;
        }
    };

    /**
     * Private constructor to prevent instantiation.
     */
    private DefaultCommitHandlers() { }
}
