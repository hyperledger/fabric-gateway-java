/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway;

import org.hyperledger.fabric.gateway.impl.event.TransactionEventSource;
import org.hyperledger.fabric.gateway.spi.Checkpointer;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.Channel;

import java.io.IOException;
import java.util.function.Consumer;

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
	 */
	Channel getChannel();

	/**
	 * Get an event source that can be used to listen for transaction events on this network.
	 * @return A transaction event source.
	 * @deprecated
	 */
	TransactionEventSource getTransactionEventSource();

	/**
	 * Add a listener to receive block events from the network.
	 * @param listener A block listener.
	 * @return The block listener argument.
	 */
	Consumer<BlockEvent> addBlockListener(Consumer<BlockEvent> listener);

	/**
	 * Add a listener to receive block events from the network with checkpointing. Re-adding a listener with the same
	 * checkpointer on subsequent application invocations will resume listening from the previous block position.
	 * @param listener A block listener.
	 * @param checkpointer Checkpointer to persist block position.
	 * @return The block listener argument.
	 * @throws IOException if an errors occurs establishing checkpointing.
	 */
	Consumer<BlockEvent> addBlockListener(Consumer<BlockEvent> listener, Checkpointer checkpointer) throws GatewayException, IOException;

//	/**
//	 * Add a listener to receive block events from the network with checkpointing. Re-adding a listener with the same
//	 * checkpointer name on subsequent application invocations will resume listening from the previous block position.
//	 * @param listener A block listener.
//	 * @param checkpointerName Name used to uniquely identify the checkpointer within this network.
//	 * @return The block listener argument.
//	 * @throws IOException if an errors occurs establishing checkpointing.
//	 */
//	Consumer<BlockEvent> addBlockListener(Consumer<BlockEvent> listener, String checkpointerName) throws GatewayException, IOException;

	/**
	 * Removes a previously added block listener. Any associated checkpointer will be closed.
	 * @param listener A block listener.
	 */
	void removeBlockListener(Consumer<BlockEvent> listener);
}