/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway;

import org.hyperledger.fabric.gateway.impl.event.TransactionEventSource;
import org.hyperledger.fabric.sdk.Channel;

/**
 * A Network object represents the set of peers in a Fabric network (channel).
 * Applications should get a Network instance from a Gateway using the
 * {@link Gateway#getNetwork(String)} method.
 *
 * @see <a href="https://hyperledger-fabric.readthedocs.io/en/release-1.4/developapps/application.html#network-channel">Developing Fabric Applications - Network Channel</a>
 */
public interface Network {
	/**
	 * Get an instance of a contract on the current network.
	 * @param chaincodeId The name of the chaincode that implements the smart contract.
	 * @return The contract object.
	 */
	Contract getContract(String chaincodeId);

	/**
	 * Get an instance of a contract on the current network.  If the chaincode instance contains more
	 * than one smart contract class (available using the latest chaincode programming model), then an
	 * individual class can be selected.
	 * @param chaincodeId The name of the chaincode that implements the smart contract.
	 * @param name The class name of the smart contract within the chaincode.
	 * @return The contract object.
	 */
	Contract getContract(String chaincodeId, String name);

	/**
	 * Get a reference to the owning Gateway connection.
	 * @return The owning gateway.
	 */
	Gateway getGateway();

	/**
	 * Get the low-level chanel object associated with this network.
	 * @return A channel.
	 * @deprecated
	 */
	Channel getChannel();

	/**
	 * Get an event source that can be used to listen for transaction events on this network.
	 * @return A transaction event source.
	 * @deprecated
	 */
	TransactionEventSource getTransactionEventSource();
}