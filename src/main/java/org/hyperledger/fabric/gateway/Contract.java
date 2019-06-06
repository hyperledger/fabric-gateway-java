/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway;

import org.hyperledger.fabric.gateway.spi.Checkpointer;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Represents a smart contract instance in a network.
 * Applications should get a Contract instance from a Network using the
 * {@link Network#getContract(String) getContract} method.
 *
 * @see <a href="https://hyperledger-fabric.readthedocs.io/en/release-1.4/developapps/application.html#construct-request">Developing Fabric Applications - Construct request</a>
 */
public interface Contract {
	/**
	 * Create an object representing a specific invocation of a transaction
	 * function implemented by this contract, and provides more control over
	 * the transaction invocation. A new transaction object <strong>must</strong>
	 * be created for each transaction invocation.
	 *
	 * @param name Transaction function name.
	 * @return A transaction object.
	 */
	Transaction createTransaction(String name);

	/**
	 * Submit a transaction to the ledger. The transaction function {@code name}
	 * will be evaluated on the endorsing peers and then submitted to the ordering service
	 * for committing to the ledger.
	 * This function is equivalent to calling {@code createTransaction(name).submit()}.
	 *
	 * @param name Transaction function name.
	 * @param args Transaction function arguments.
	 * @return Payload response from the transaction function.
	 * @throws GatewayException
	 * @throws TimeoutException If the transaction was successfully submitted to the orderer but
	 * timed out before a commit event was received from peers.
	 *
	 * @see <a href="https://hyperledger-fabric.readthedocs.io/en/release-1.4/developapps/application.html#submit-transaction">Developing Fabric Applications - Submit transaction</a>
	 */
	byte[] submitTransaction(String name, String... args) throws GatewayException, TimeoutException;

	/**
	 * Evaluate a transaction function and return its results.
	 * The transaction function {@code name}
	 * will be evaluated on the endorsing peers but the responses will not be sent to
	 * the ordering service and hence will not be committed to the ledger.
	 * This is used for querying the world state.
	 * This function is equivalent to calling {@code createTransaction(name).evaluate()}.
	 *
	 * @param name Transaction function name.
	 * @param args Transaction function arguments.
	 * @return Payload response from the transaction function.
	 * @throws GatewayException
	 */
	byte[] evaluateTransaction(String name, String... args) throws GatewayException;

	/**
	 * Add a listener to receive all contract events emitted by transactions.
	 * @param listener A contract listener.
	 * @return The contract listener argument.
	 */
	Consumer<ContractEvent> addContractListener(Consumer<ContractEvent> listener);

	/**
	 * Add a listener to receive contract events emitted by transactions. The listener is only notified of events
	 * with exactly the given name.
	 * @param listener A contract listener.
	 * @param eventName Event name.
	 * @return The contract listener argument.
	 */
	Consumer<ContractEvent> addContractListener(Consumer<ContractEvent> listener, String eventName);

	/**
	 * Add a listener to receive contract events emitted by transactions. The listener is only notified of events
	 * with names that entirely match the given pattern.
	 * @param listener A contract listener.
	 * @param eventNamePattern Event name pattern.
	 * @return The contract listener argument.
	 */
	Consumer<ContractEvent> addContractListener(Consumer<ContractEvent> listener, Pattern eventNamePattern);

	/**
	 * Add a listener to receive all contract events emitted by transactions with checkpointing. Re-adding a listener
	 * with the same checkpointer on subsequent application invocations will resume listening from the previous block
	 * and transaction position.
	 * @param checkpointer Checkpointer to persist block and transaction position.
	 * @param listener A contract listener.
	 * @return The contract listener argument.
	 */
	Consumer<ContractEvent> addContractListener(Checkpointer checkpointer, Consumer<ContractEvent> listener) throws IOException, GatewayException;

	/**
	 * Add a listener to receive contract events emitted by transactions with checkpointing. The listener is only
	 * notified of events with names that exactly match the given pattern. Re-adding a listener with the same
	 * checkpointer on subsequent application invocations will resume listening from the previous block and transaction
	 * position.
	 * @param checkpointer Checkpointer to persist block and transaction position.
	 * @param listener A contract listener.
	 * @param eventName Event name.
	 * @return The contract listener argument.
	 */
	Consumer<ContractEvent> addContractListener(Checkpointer checkpointer, Consumer<ContractEvent> listener, String eventName) throws IOException, GatewayException;

	/**
	 * Add a listener to receive contract events emitted by transactions with checkpointing. The listener is only
	 * notified of events with names that entirely match the given pattern. Re-adding a listener with the same
	 * checkpointer on subsequent application invocations will resume listening from the previous block and transaction
	 * position.
	 * @param checkpointer Checkpointer to persist block and transaction position.
	 * @param listener A contract listener.
	 * @param eventNamePattern Event name pattern.
	 * @return The contract listener argument.
	 */
	Consumer<ContractEvent> addContractListener(Checkpointer checkpointer, Consumer<ContractEvent> listener, Pattern eventNamePattern) throws IOException, GatewayException;

	/**
	 * Remove a previously registered contract listener.
	 * @param listener A contract listener.
	 */
	void removeContractListener(Consumer<ContractEvent> listener);
}
