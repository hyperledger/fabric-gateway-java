/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway;

import java.nio.file.Path;

import org.hyperledger.fabric.gateway.impl.GatewayImpl;
import org.hyperledger.fabric.gateway.spi.CommitHandlerFactory;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.NetworkConfig;

/**
 * The gateway peer provides the connection point for an application to access the Fabric network.
 * It is instantiated from a Builder instance that is created using {@link #createBuilder()} and configured
 * using either a {@link NetworkConfig} instance and a {@link Wallet} identity, or and existing {@link HFClient} object.
 * It can then be connected to a fabric network using the {@link Builder#connect()} method.
 * Once connected, it can then access individual Network instances (channels) using the {@link #getNetwork(String)} method
 * which in turn can access the {@link Contract} installed on a network and
 * [submit transactions]{@link Contract#submitTransaction(String, String...)} to the ledger.
 */
public interface Gateway extends AutoCloseable {

  /**
   * Returns an object representing a network
   * @param {string} networkName The name of the network (channel name)
   * @returns {@link Network}
   */
Network getNetwork(String networkName) throws GatewayException;

  /**
   * Get the identity associated with the gateway connection.
   *
   * @returns {@link Wallet.Identity}} The identity used by this Gateway.
   */
  Wallet.Identity getIdentity();

  static Builder createBuilder() {
    return new GatewayImpl.Builder();
  }

  interface Builder {
    Builder networkConfig(Path config);
    Builder identity(Wallet wallet, String id) throws GatewayException;
    Builder commitHandler(CommitHandlerFactory commitHandler);

    Gateway connect() throws GatewayException;
  }
}
