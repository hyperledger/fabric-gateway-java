/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.spi;

import java.io.IOException;
import java.util.Set;

import org.hyperledger.fabric.gateway.DefaultCheckpointers;

/**
 * Persists the current block and transactions within that block to enable event listening to be resumed following an
 * application outage.
 * <p>Default implementations can be obtained from {@link DefaultCheckpointers}. Application developers are
 * encouraged to build their own implementations that use a persistent store suitable to their environment.</p>
 * <p>Implementations must be thread-safe.</p>
 */
public interface Checkpointer extends AutoCloseable {
    /**
     * Block number indicating no stored value. This is the the default for a newly created checkpointer with no
     * previously saved state.
     */
    long UNSET_BLOCK_NUMBER = -1;

    /**
     * Get the current block number, or {@link #UNSET_BLOCK_NUMBER} if there is no previously saved state.
     * @return A block number.
     * @throws IOException if the checkpointer fails to access persistent state.
     */
    long getBlockNumber() throws IOException;

    /**
     * Set the current block number. Also clears the stored transaction IDs. Typically set when the previous block has
     * been processed.
     * @param blockNumber A block number.
     * @throws IOException if the checkpointer fails to access persistent state.
     */
    void setBlockNumber(long blockNumber) throws IOException;

    /**
     * Get the transaction IDs processed within the current block.
     * @return Transaction IDs.
     * @throws IOException if the checkpointer fails to access persistent state.
     */
    Set<String> getTransactionIds() throws IOException;

    /**
     * Add a transaction ID for the current block. Typically called once a transaction has been processed.
     * @param transactionId A transaction ID.
     * @throws IOException if the checkpointer fails to access persistent state.
     */
    void addTransactionId(String transactionId) throws IOException;

    @Override
    void close() throws IOException;
}
