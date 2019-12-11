/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway;

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
        return new WalletImpl(store);
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
