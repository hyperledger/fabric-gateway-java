/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.spi;

import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.DefaultQueryHandlers;

/**
 * Functional interface describing a factory function for constructing {@link QueryHandler} instances.
 * <p>Default implementations can be obtained from {@link DefaultQueryHandlers}.</p>
 */
@FunctionalInterface
public interface QueryHandlerFactory {
  /**
   * Factory function to create a query handler instance.
   * @param network Network on which the query is invoked.
   * @return A query handler.
   */
  QueryHandler create(Network network);
}
