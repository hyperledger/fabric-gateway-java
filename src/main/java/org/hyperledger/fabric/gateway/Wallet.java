/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway;

import java.io.IOException;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.util.Set;

import org.hyperledger.fabric.gateway.impl.FileSystemWallet;
import org.hyperledger.fabric.gateway.impl.InMemoryWallet;
import org.hyperledger.fabric.gateway.impl.WalletIdentity;

public interface Wallet {
  static Wallet createFileSystemWallet(Path basePath) throws IOException {
    return new FileSystemWallet(basePath);
  }

  static Wallet createInMemoryWallet() {
    return new InMemoryWallet();
  }

  interface Identity {
    /**
     * Get the MSP ID.
     *
     * @return a MSP ID.
     */
    String getMspId();

    /**
     * Get the user's signed certificate.
     *
     * @return a certificate.
     */
    String getCertificate();

    /**
     * Get the user's private key
     *
     * @return private key.
     */
    PrivateKey getPrivateKey();

    static Identity createIdentity(String mspId, String certificate, PrivateKey pk) {
      return new WalletIdentity(mspId, certificate, pk);
    }
}

  void put(String label, Identity identity) throws GatewayException;

  Identity get(String label) throws GatewayException;

  Set<String> getAllLabels() throws GatewayException;

  void remove(String label) throws GatewayException;

  boolean exists(String label) throws GatewayException;
}
