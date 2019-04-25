/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway;

import java.util.concurrent.TimeoutException;

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
}
