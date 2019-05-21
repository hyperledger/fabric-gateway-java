/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import org.hyperledger.fabric.gateway.Wallet;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class InMemoryWallet implements Wallet {
  Map<String, Identity> store = new HashMap<String, Identity>();

  @Override
  public void put(String label, Identity identity) {
    store.put(label, identity);
  }

  @Override
  public Identity get(String label) {
    return store.get(label);
  }

  @Override
  public Set<String> getAllLabels() {
    return store.keySet();
  }

  @Override
  public void remove(String label) {
    store.remove(label);
  }

  @Override
  public boolean exists(String label) {
    return store.containsKey(label);
  }

}
