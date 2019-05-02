/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A Transaction represents a specific invocation of a transaction function, and provides
 * flexibility over how that transaction is invoked. Applications should
 * obtain instances of this class from a Contract using the
 * {@link Contract#createTransaction(String) createTransaction} method.
 * <br>
 * Instances of this class are stateful. A new instance <strong>must</strong>
 * be created for each transaction invocation.
 *
 *
 */
public interface Transaction {
	/**
	 * Get the fully qualified name of the transaction function.
	 * @return Transaction name.
	 */
	String getName();

	/**
	 * Set transient data that will be passed to the transaction function
	 * but will not be stored on the ledger. This can be used to pass
	 * private data to a transaction function.
	 *
	 * @param transientData A Map containing the transient data.
	 */
	Transaction setTransient(Map<String, byte[]> transientData);

	/**
	 * Set the maximum length of time to wait for commit events to be received after submitting a transaction to the
	 * orderer.
	 * @param timeout the maximum time to wait.
	 * @param timeUnit the time unit of the timeout argument.
	 */
	Transaction setCommitTimeout(long timeout, TimeUnit timeUnit);

	/**
	 * Submit a transaction to the ledger. The transaction function represented by this object
	 * will be evaluated on the endorsing peers and then submitted to the ordering service
	 * for committing to the ledger.
	 *
	 * @param args Transaction function arguments.
	 * @return Payload response from the transaction function.
	 * @throws GatewayException if a error occurs submitting the transaction to the ledger.
	 * @throws TimeoutException if the transaction was successfully submitted to the orderer but
	 * timed out before a commit event was received from peers.
	 */
	byte[] submit(String... args) throws GatewayException, TimeoutException;

	/**
	 * Evaluate a transaction function and return its results.
	 * The transaction function will be evaluated on the endorsing peers but
	 * the responses will not be sent to the ordering service and hence will
	 * not be committed to the ledger.
	 * This is used for querying the world state.
	 *
	 * @param args Transaction function arguments.
	 * @return Payload response from the transaction function.
	 * @throws GatewayException
	 */
	byte[] evaluate(String... args) throws GatewayException;
}
