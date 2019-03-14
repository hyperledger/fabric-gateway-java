/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway;

import java.nio.file.Path;

import org.hyperledger.fabric.gateway.impl.GatewayImpl;
import org.hyperledger.fabric.gateway.spi.CommitHandlerFactory;

/**
 * The Gateway provides the connection point for an application to access
 * the Fabric network. It is instantiated from a Builder instance that is
 * created using {@link #createBuilder()} and configured using a common
 * connection profile and a {@link Wallet} identity. It can then be connected to
 * a fabric network using the {@link Builder#connect()} method. Once connected,
 * it can then access individual {@link Network} instances (channels) using the
 * {@link #getNetwork(String) getNetwork} method which in turn can access the
 * {@link Contract} installed on a network and
 * {@link Contract#submitTransaction(String, String...) submit transactions} to
 * the ledger.
 *
 * @see <a href="https://hyperledger-fabric.readthedocs.io/en/release-1.4/developapps/application.html#gateway">Developing Fabric Applications - Gateway</a>
 */
public interface Gateway extends AutoCloseable {

	/**
	 * Returns an object representing a network
	 *
	 * @param networkName The name of the network (channel name)
	 * @return {@link Network}
	 * @throws GatewayException Contains details of the Fabric error
	 */
	Network getNetwork(String networkName) throws GatewayException;

	/**
	 * Get the identity associated with the gateway connection.
	 *
	 * @return {@link Wallet.Identity}} The identity used by this Gateway.
	 */
	Wallet.Identity getIdentity();

	/**
	 * Creates a gateway builder which is used to configure the gateway options
	 * prior to connecting to the Fabric network.
	 *
	 * @return A gateway connection.
	 */
	static Builder createBuilder() {
		return new GatewayImpl.Builder();
	}

	/**
	 *
	 * The Gateway Builder interface defines the options that can be configured
	 * prior to connection.
	 * An instance of builder is created using the static method
	 * {@link Gateway#createBuilder()}.  Every method on the builder object will return
	 * a reference to the same builder object allowing them to be chained together in
	 * a single line.
	 *
	 */
	interface Builder {

		/**
		 * Specifies the path to the common connection profile.
		 * @param config The path to the common connection profile.
		 * @return The builder instance, allowing multiple configuration options to be chained.
		 */
		Builder networkConfig(Path config);

		/**
		 * Specifies the identity that is to be used to connect to the network.  All operations
		 * under this gateway connection will be performed using this identity.
		 * @param wallet The {@link Wallet} object containing the identity.
		 * @param id The name of the identity stored in the wallet.
		 * @return The builder instance, allowing multiple configuration options to be chained.
		 * @throws GatewayException
		 */
		Builder identity(Wallet wallet, String id) throws GatewayException;

		/**
		 * Allows an alternative commit handler to be specified. Optional.
		 * @param commitHandler
		 * @return The builder instance, allowing multiple configuration options to be chained.
		 */
		Builder commitHandler(CommitHandlerFactory commitHandler);

		/**
		 * Connects to the gateway using the specified options.
		 * @return The connected {@link Gateway} object.
		 * @throws GatewayException
		 */
		Gateway connect() throws GatewayException;
	}
}
