/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

import org.hyperledger.fabric.gateway.impl.identity.CloudantWalletStore;
import org.hyperledger.fabric.gateway.impl.identity.FileSystemWalletStore;
import org.hyperledger.fabric.gateway.impl.identity.InMemoryWalletStore;
import org.hyperledger.fabric.gateway.impl.identity.WalletImpl;
import org.hyperledger.fabric.gateway.spi.WalletStore;

/**
 * Factory methods for creating wallets to hold identity information, using various backing stores.
 */
public final class Wallets {
    /**
     * Create a wallet backed by an in-memory (non-persistent) store. Each wallet instance created will have its own
     * private in-memory store.
     * @return A wallet.
     */
    public static Wallet newInMemoryWallet() {
        WalletStore store = new InMemoryWalletStore();
        return newWallet(store);
    }

    /**
     * Create a wallet backed by a directory on the file system.
     * @param storeDirectory A directory.
     * @return A wallet.
     * @throws IOException if the specified directory does not exist and can not be created.
     */
    public static Wallet newFileSystemWallet(final Path storeDirectory) throws IOException {
        WalletStore store = new FileSystemWalletStore(storeDirectory);
        return newWallet(store);
    }

    /**
     * Create a wallet backed by a CouchDB database.
     * @param serverUrl Connection URL for CouchDB server.
     * @param databaseName Wallet database name.
     * @return A wallet.
     */
    public static Wallet newCouchDBWallet(final URL serverUrl, final String databaseName) {
        WalletStore store = CloudantWalletStore.newInstance(serverUrl, databaseName);
        return newWallet(store);
    }

    /**
     * Create a wallet backed by a custom store implementation.
     * @param store  A wallet store implementation.
     * @return A wallet.
     */
    public static Wallet newWallet(final WalletStore store) {
        return new WalletImpl(store);
    }

    // Private constructor to prevent instantiation
    private Wallets() { }
}
