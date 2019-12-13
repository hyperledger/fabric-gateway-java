/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.identity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.hyperledger.fabric.gateway.Identities;
import org.hyperledger.fabric.gateway.Identity;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallets;
import org.hyperledger.fabric.gateway.X509Credentials;
import org.hyperledger.fabric.gateway.spi.WalletStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public final class WalletTest {
    private WalletStore store;
    private Wallet wallet;
    private final X509Credentials credentials = new X509Credentials();
    private final Identity identity = Identities.newX509Identity("mspId", credentials.getCertificate(), credentials.getPrivateKey());

    @BeforeEach
    public void beforeEach() {
        store = new InMemoryWalletStore();
        wallet = Wallets.newWallet(store);
    }

    @Test
    public void list_empty_wallet_returns_empty_set() throws IOException {
        Set<String> result = wallet.list();
        assertThat(result).isEmpty();
    }

    @Test
    public void list_returns_stored_identities() throws IOException {
        wallet.put("alice", identity);

        Set<String> result = wallet.list();

        assertThat(result).containsExactly("alice");
    }

    @Test
    public void list_does_not_return_deleted_identities() throws IOException {
        wallet.put("alice", identity);
        wallet.remove("alice");

        Set<String> result = wallet.list();

        assertThat(result).isEmpty();
    }

    @Test
    public void put_unsupported_identity_type_throws_IllegalArgumentException() {
        Identity unsupportedIdentity = new Identity() {
            @Override
            public String getMspId() {
                return "mspId";
            }
        };

        assertThatThrownBy(() -> wallet.put("alice", unsupportedIdentity))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(unsupportedIdentity.getClass().getName());
    }

    @Test
    public void get_invalid_identity_returns_null() throws IOException {
        Identity result = wallet.get("alice");
        assertThat(result).isNull();
    }

    @Test
    public void get_returns_stored_identities() throws IOException {
        wallet.put("alice", identity);

        Identity result = wallet.get("alice");

        assertThat(result).isEqualTo(identity);
    }

    @Test
    public void get_identity_with_bad_persistent_data_throws_IOException() throws IOException {
        InputStream dataInput = new ByteArrayInputStream("Bad data".getBytes(StandardCharsets.UTF_8));
        store.put("label", dataInput);

        assertThatThrownBy(() -> wallet.get("label"))
                .isInstanceOf(IOException.class);
    }
}
