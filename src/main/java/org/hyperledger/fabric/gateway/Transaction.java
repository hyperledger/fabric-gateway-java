/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.hyperledger.fabric.sdk.Peer;

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
     * @param transientData A Map containing the transient data.
     * @return this transaction object to allow method chaining.
     */
    Transaction setTransient(Map<String, byte[]> transientData);

    /**
     * Set the maximum length of time to wait for commit events to be received after submitting a transaction to the
     * orderer.
     * @param timeout the maximum time to wait.
     * @param timeUnit the time unit of the timeout argument.
     * @return this transaction object to allow method chaining.
     */
    Transaction setCommitTimeout(long timeout, TimeUnit timeUnit);

    /**
     * Set the peers that should be used for endorsement of transaction submitted to the ledger using
     * {@link #submit(String...)}.
     * @param peers Endorsing peers.
     * @return this transaction object to allow method chaining.
     */
    Transaction setEndorsingPeers(Collection<Peer> peers);

    /**
     * Submit a transaction to the ledger. The transaction function represented by this object
     * will be evaluated on the endorsing peers and then submitted to the ordering service
     * for committing to the ledger.
     *
     * @param args Transaction function arguments.
     * @return Payload response from the transaction function.
     * @throws ContractException if the transaction is rejected.
     * @throws TimeoutException if the transaction was successfully submitted to the orderer but
     * timed out before a commit event was received from peers.
     * @throws InterruptedException if the current thread is interrupted while waiting.
     * @throws GatewayRuntimeException if an underlying infrastructure failure occurs.
     */
    byte[] submit(String... args) throws ContractException, TimeoutException, InterruptedException;

    /**
     * Evaluate a transaction function and return its results.
     * The transaction function will be evaluated on the endorsing peers but
     * the responses will not be sent to the ordering service and hence will
     * not be committed to the ledger. This is used for querying the world state.
     *
     * @param args Transaction function arguments.
     * @return Payload response from the transaction function.
     * @throws ContractException if no peers are reachable or an error response is returned.
     * @throws GatewayRuntimeException if an underlying infrastructure failure occurs.
     */
    byte[] evaluate(String... args) throws ContractException;
}
