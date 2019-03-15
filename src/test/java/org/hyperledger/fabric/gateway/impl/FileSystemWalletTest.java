/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import org.hyperledger.fabric.gateway.Wallet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

public class FileSystemWalletTest extends WalletTest {
  @TempDir
  public Path basePath;

  @Override
  @BeforeEach
  public void setup() throws Exception {
    super.setup();
    wallet = Wallet.createFileSystemWallet(basePath);
  }

  @Test
  public void testCreateFolder() throws Exception {
    // create a wallet getInstance for non-existing folder
    Path tempDir = basePath.resolve("temp");
    Wallet existing = Wallet.createFileSystemWallet(tempDir);
  }

}
