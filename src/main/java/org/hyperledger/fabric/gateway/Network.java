/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway;

import java.io.IOException;
import java.util.Collection;
import java.util.function.Consumer;

import org.hyperledger.fabric.gateway.spi.Checkpointer;
import org.hyperledger.fabric.gateway.spi.CommitListener;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.Peer;

/**
 * A Network object represents the set of peers in a Fabric network (channel).
 * Applications should get a Network instance from a Gateway using the
 * {@link Gateway#getNetwork(String)} method.
 *
 * <p>The Network object provides the ability for applications to:
 * <ul>
 *     <li>Obtain a specific smart contract deployed to the network using {@link #getContract(String)}, in order to
 *     submit and evaluate transactions for that smart contract.</li>
 *     <li>Listen to new block events using {@link #addBlockListener(Consumer)}.</li>
 *     <li>Replay previous block events using {@link #addBlockListener(long, Consumer)}.</li>
 * </ul></p>
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
	 * Add a listener to receive block events from the network. Events are received in order and without duplication.
	 * @param listener A block listener.
	 * @return The block listener argument.
	 */
	Consumer<BlockEvent> addBlockListener(Consumer<BlockEvent> listener);

	/**
	 * Add a listener to receive block events from the network with checkpointing. Re-adding a listener with the same
	 * checkpointer on subsequent application invocations will resume listening from the previous block position.
	 * Events are received in order and without duplication.
	 * @param checkpointer Checkpointer to persist block position.
	 * @param listener A block listener.
	 * @return The block listener argument.
	 * @throws IOException if an error occurs establishing checkpointing.
	 * @throws GatewayRuntimeException if an underlying infrastructure failure occurs.
	 */
	Consumer<BlockEvent> addBlockListener(Checkpointer checkpointer, Consumer<BlockEvent> listener) throws IOException;

	/**
	 * Add a listener to replay block events from the network. Events are received in order and without duplication.
	 * @param startBlock The number of the block from which events should be replayed.
	 * @param listener A block listener.
	 * @return The block listener argument.
	 * @throws GatewayRuntimeException if an underlying infrastructure failure occurs.
	 */
	Consumer<BlockEvent> addBlockListener(long startBlock, Consumer<BlockEvent> listener);

	/**
	 * Removes a previously added block listener.
	 * @param listener A block listener.
	 */
	void removeBlockListener(Consumer<BlockEvent> listener);

	/**
	 * Add a listener to receive transaction commit and peer disconnect events for a set of peers.
	 * @param listener A transaction commit listener.
	 * @param peers The peers from which to receive events.
	 * @param transactionId A transaction ID.
	 * @return The transaction commit listener argument.
	 */
	CommitListener addCommitListener(CommitListener listener, Collection<Peer> peers, String transactionId);

	/**
	 * Removes a previously added transaction commit listener.
	 * @param listener A block listener.
	 */
	void removeCommitListener(CommitListener listener);
}