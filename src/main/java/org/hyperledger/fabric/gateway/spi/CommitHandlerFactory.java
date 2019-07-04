/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.spi;

import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.DefaultCommitHandlers;

/**
 * Functional interface describing a factory function for constructing {@link CommitHandler} instances.
 * <p>Default implementations can be obtained from {@link DefaultCommitHandlers}.</p>
 */
@FunctionalInterface
public interface CommitHandlerFactory {
    /**
     * Factory function to create a commit handler instance.
     * @param transactionId Transaction for which the instance is to handle commit events.
     * @param network Network on which the transaction is invoked.
     * @return A commit handler.
     */
    CommitHandler create(String transactionId, Network network);
}
