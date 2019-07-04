/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.hyperledger.fabric.gateway.impl.GatewayImpl;
import org.hyperledger.fabric.gateway.spi.CommitHandlerFactory;
import org.hyperledger.fabric.gateway.spi.QueryHandlerFactory;

/**
 * The Gateway provides the connection point for an application to access the Fabric network as a specific user. It is
 * instantiated from a Builder instance that is created using {@link #createBuilder()} and configured using a common
 * connection profile and a {@link Wallet} identity. It can then be connected to a fabric network using the
 * {@link Builder#connect()} method. Once connected, it can then access individual {@link Network} instances (channels)
 * using the {@link #getNetwork(String) getNetwork} method which in turn can access the {@link Contract} installed on a
 * network and {@link Contract#submitTransaction(String, String...) submit transactions} to the ledger.
 *
 * <p>Gateway instances should be closed only once connection to the Fabric network is no longer required.</p>
 *
 * <pre><code>
 *     Gateway.Builder builder = Gateway.createBuilder()
 *             .identity(wallet, "user1")
 *             .networkConfig(networkConfigFile);
 *
 *     try (Gateway gateway = builder.connect()) {
 *         Network network = gateway.getNetwork("mychannel");
 *         // Interactions with the network
 *     }
 * </code></pre>
 *
 * @see <a href="https://hyperledger-fabric.readthedocs.io/en/release-1.4/developapps/application.html#gateway">Developing Fabric Applications - Gateway</a>
 */
public interface Gateway extends AutoCloseable {
	/**
	 * Returns an object representing a network
	 *
	 * @param networkName The name of the network (channel name)
	 * @return {@link Network}
	 * @throws GatewayRuntimeException if a configuration or infrastructure error causes a failure.
	 */
	Network getNetwork(String networkName);

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
	 * Close the gateway connection and all associated resources, including removing listeners attached to networks and
	 * contracts created by the gateway.
	 */
	void close();

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
		 * @throws IOException if the config file does not exist, or is not JSON or YAML format,
		 * or contains invalid information.
		 */
		Builder networkConfig(Path config) throws IOException;

		/**
		 * Specifies the identity that is to be used to connect to the network.  All operations
		 * under this gateway connection will be performed using this identity.
		 * @param wallet The {@link Wallet} object containing the identity.
		 * @param id The name of the identity stored in the wallet.
		 * @return The builder instance, allowing multiple configuration options to be chained.
		 * @throws IOException if the specified identity can not be loaded from the wallet.
		 */
		Builder identity(Wallet wallet, String id) throws IOException;

		/**
		 * <em>Optional</em> - Allows an alternative commit handler to be specified. The commit handler defines how
		 * client code should wait to receive commit events from peers following submit of a transaction.
		 * <p>Default commit handler implementations are defined in {@link DefaultCommitHandlers}.</p>
		 * @param commitHandler A commit handler implementation.
		 * @return The builder instance, allowing multiple configuration options to be chained.
		 */
		Builder commitHandler(CommitHandlerFactory commitHandler);

		/**
		 * <em>Optional</em> - Allows an alternative query handler to be specified. The query handler defines the
		 * peers that should be used to evaluate transactions.
		 * <p>Default query handler implementations are defined in {@link DefaultQueryHandlers}.</p>
		 * @param queryHandler A query handler implementation.
		 * @return The builder instance, allowing multiple configuration options to be chained.
		 */
		Builder queryHandler(QueryHandlerFactory queryHandler);

		/**
		 * <em>Optional</em> - Set the default maximum time to wait for commit events to be received from peers after
		 * submitting a transaction to the orderer.
		 * @param timeout the maximum time to wait.
		 * @param timeUnit the time unit of the timeout argument.
		 * @return The builder instance, allowing multiple configuration options to be chained.
		 */
		Builder commitTimeout(long timeout, TimeUnit timeUnit);

		/**
		 * <em>Optional</em> - Enable or disable service discovery for all transaction submissions for this gateway
		 * @param enabled - true to enable service discovery
		 * @return The builder instance, allowing multiple configuration options to be chained.
		 */
		Builder discovery(boolean enabled);

		/**
		 * Connects to the gateway using the specified options.
		 * @return The connected {@link Gateway} object.
		 */
		Gateway connect();
	}
}
