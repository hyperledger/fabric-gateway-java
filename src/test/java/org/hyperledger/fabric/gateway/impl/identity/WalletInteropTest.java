/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.identity;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Stream;

import org.hyperledger.fabric.gateway.Identity;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallets;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;


public final class WalletInteropTest {
    public static final Path walletPath = Paths.get("src", "test", "fixtures", "test-wallet");
    public static Wallet wallet;

    @BeforeAll
    public static void createWallet() throws IOException {
        wallet = Wallets.newFileSystemWallet(walletPath);
    }

    public static Stream<String> labelSupplier() throws IOException {
        return wallet.list().stream();
    }

    @Test
    public void wallet_is_not_empty() throws IOException {
        Set<String> result = wallet.list();
        assertThat(result).isNotEmpty();
    }

    @ParameterizedTest
    @MethodSource("labelSupplier")
    public void can_get_identity(String label) throws IOException {
        Identity result = wallet.get(label);
        assertThat(result).isNotNull();
    }
}
