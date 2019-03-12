/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.spi;

import java.util.concurrent.CompletableFuture;

public interface QueryHandler {
  CompletableFuture<String> evaluate(Query query);
}
