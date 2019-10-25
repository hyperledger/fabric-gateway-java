/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import java.security.PrivateKey;

import org.hyperledger.fabric.gateway.Wallet;

public final class WalletIdentity implements Wallet.Identity {
  private final String mspId;
  private final String certificate;
  private final PrivateKey privateKey;

  public WalletIdentity(final String mspId, final String certificate, final PrivateKey privateKey) {
    this.mspId = mspId;
    this.certificate = certificate;
    this.privateKey = privateKey;
  }

  @Override
  public String getMspId() {
    return this.mspId;
  }

  @Override
  public String getCertificate() {
    return this.certificate;
  }

  @Override
  public PrivateKey getPrivateKey() {
    return this.privateKey;
  }

}
