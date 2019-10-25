/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hyperledger.fabric.gateway.Wallet;

public final class InMemoryWallet implements Wallet {
  private Map<String, Identity> store = new HashMap<>();

  @Override
  public void put(final String label, final Identity identity) {
    store.put(label, identity);
  }

  @Override
  public Identity get(final String label) {
    return store.get(label);
  }

  @Override
  public Set<String> getAllLabels() {
    return store.keySet();
  }

  @Override
  public void remove(final String label) {
    store.remove(label);
  }

  @Override
  public boolean exists(final String label) {
    return store.containsKey(label);
  }

}
