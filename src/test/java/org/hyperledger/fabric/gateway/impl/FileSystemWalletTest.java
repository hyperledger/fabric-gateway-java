/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import java.nio.file.Path;

import org.hyperledger.fabric.gateway.Wallet;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FileSystemWalletTest extends WalletTest {

  @Rule
  public TemporaryFolder basePath = new TemporaryFolder();

  @Override
  @Before
  public void setup() throws Exception {
    super.setup();
    wallet = Wallet.createFileSystemWallet(basePath.getRoot().toPath());
  }

  @Test
  public void testCreateFolder() throws Exception {
    // create a wallet instance for non-existing folder
    Path tempDir = basePath.getRoot().toPath().resolve("temp");
    Wallet existing = Wallet.createFileSystemWallet(tempDir);
  }

}
