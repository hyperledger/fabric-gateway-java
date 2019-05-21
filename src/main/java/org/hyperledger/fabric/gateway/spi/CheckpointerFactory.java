/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.spi;

import java.io.IOException;

/**
 * Factory for creating {@link Checkpointer} instances.
 */
public interface CheckpointerFactory {
    /**
     * Factory function to create a checkpointer instance. The created checkpointer will retain the persisted data
     * stored by previous checkpointer instances of the same name for the given network.
     * @param networkName Name of the network for which event information is to be persisted.
     * @param checkpointerName Name that uniquely identifies a checkpointer within a network.
     * @return A checkpointer.
     * @throws IOException if an error occurs connecting to the persistent store.
     */
    Checkpointer create(String networkName, String checkpointerName) throws IOException;
}
