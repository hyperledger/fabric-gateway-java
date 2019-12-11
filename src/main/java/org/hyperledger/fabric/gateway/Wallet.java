/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway;

import java.io.IOException;
import java.util.Set;

/**
 * A wallet stores identity information used to connect to a Hyperledger Fabric network. Instances are created using
 * factory methods on {@link Wallets}.
 */
public interface Wallet {
    /**
     * Put an identity into the wallet.
     * @param label Label used to identify the identity within the wallet.
     * @param identity Identity to store in the wallet.
     * @throws IOException if an error occurs accessing the backing store.
     */
    void put(String label, Identity identity) throws IOException;

    /**
     * Get an identity from the wallet. The implementation class of the identity object will vary depending on its type.
     * @param label Label used to identify the identity within the wallet.
     * @return An identity if it exists; otherwise null.
     * @throws IOException if an error occurs accessing the backing store.
     */
    Identity get(String label) throws IOException;

    /**
     * Get the labels of all identities in the wallet.
     * @return Identity labels.
     * @throws IOException if an error occurs accessing the backing store.
     */
    Set<String> list() throws IOException;

    /**
     * Remove an identity from the wallet. If the identity does not exist, this method does nothing.
     * @param label Label used to identify the identity within the wallet.
     * @throws IOException if an error occurs accessing the backing store.
     */
    void remove(String label) throws IOException;
}
