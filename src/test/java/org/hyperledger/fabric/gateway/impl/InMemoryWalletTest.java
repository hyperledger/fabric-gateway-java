/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import org.hyperledger.fabric.gateway.Wallet;
import org.junit.Before;

public class InMemoryWalletTest extends WalletTest {

  @Override
  @Before
  public void setup() throws Exception {
    super.setup();
    wallet = Wallet.createInMemoryWallet();
  }

}
