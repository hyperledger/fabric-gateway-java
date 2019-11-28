/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.identity;

import org.hyperledger.fabric.gateway.spi.WalletStore;

public class InMemoryWalletStoreTest extends CommonWalletStoreTest {
    @Override
    protected WalletStore newWalletStore() {
        return new InMemoryWalletStore();
    }
}
