/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.spi;

import org.hyperledger.fabric.gateway.Network;

/**
 * Functional interface describing a factory function for constructing {@link CommitHandler} instances.
 * <p>Default implementations can be obtained from {@link org.hyperledger.fabric.gateway.DefaultCommitHandlers}.</p>
 * @see <a href="https://github.com/hyperledger/fabric-gateway-java/blob/main/src/test/java/org/hyperledger/fabric/gateway/sample/SampleCommitHandlerFactory.java">SampleCommitHandlerFactory</a>
 * @see <a href="https://github.com/hyperledger/fabric-gateway-java/blob/main/src/test/java/org/hyperledger/fabric/gateway/sample/SampleCommitHandler.java">SampleCommitHandler</a>
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
