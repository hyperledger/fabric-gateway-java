/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.spi;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.hyperledger.fabric.sdk.Peer;

public interface Query {
  CompletableFuture<Map<String, String>> evaluate(List<Peer> peers);
}
