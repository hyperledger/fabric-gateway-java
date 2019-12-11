/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.spi;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * Interface for store implementations that provide backing storage for identities in a
 * {@link org.hyperledger.fabric.gateway.Wallet}.
 */
public interface WalletStore {
    /**
     * Remove data from the store. If the data does not exist, this does nothing.
     * @param label Name used to key the data.
     * @throws IOException if an error occurs accessing underlying persistent storage.
     */
    void remove(String label) throws IOException;

    /**
     * Get data from the store.
     * @param label Name used to key the data.
     * @return The data, if it exists; otherwise null.
     * @throws IOException if an error occurs accessing underlying persistent storage.
     */
    InputStream get(String label) throws IOException;

    /**
     * List the labels for all stored data.
     * @return Labels.
     * @throws IOException if an error occurs accessing underlying persistent storage.
     */
    Set<String> list() throws IOException;

    /**
     * Put data into the store. If data already exists for this label, it is overwritten.
     * @param label Name used to key the data.
     * @param data Data to be stored.
     * @throws IOException if an error occurs accessing underlying persistent storage.
     */
    void put(String label, InputStream data) throws IOException;
}
