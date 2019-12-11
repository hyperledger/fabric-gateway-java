/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.identity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.hyperledger.fabric.gateway.impl.GatewayUtils;
import org.hyperledger.fabric.gateway.spi.WalletStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class CommonWalletStoreTest {
    protected WalletStore store;

    protected abstract WalletStore newWalletStore() throws IOException;

    protected InputStream asInputStream(String data) {
        return new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
    }

    protected String asString(InputStream dataInput) {
        try (ByteArrayOutputStream byteOutput = new ByteArrayOutputStream()) {
            GatewayUtils.copy(dataInput, byteOutput);
            return new String(byteOutput.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @BeforeEach
    public void beforeEach() throws IOException {
        store = newWalletStore();
    }

    @Test
    public void list_on_empty_store_returns_empty_set() throws IOException {
        Set<String> results = store.list();

        assertThat(results).isEmpty();
    }

    @Test
    public void list_returns_labels() throws IOException {
        store.put("label", asInputStream("data"));

        Set<String> results = store.list();

        assertThat(results).containsExactly("label");
    }

    @Test
    public void get_invalid_label_returns_null() throws IOException {
        InputStream result = store.get("label");

        assertThat(result).isNull();
    }


    @Test
    public void get_valid_label_returns_stored_data() throws IOException {
        store.put("label", asInputStream("data"));

        InputStream result = store.get("label");

        assertThat(asString(result)).isEqualTo("data");
    }

    @Test
    public void list_does_not_return_deleted_data() throws IOException {
        store.put("label", asInputStream("data"));
        store.remove("label");

        Set<String> results = store.list();

        assertThat(results).isEmpty();
    }

    @Test
    public void delete_invalid_label_does_nothing() throws IOException {
        store.put("label", asInputStream("data"));
        store.remove("invalid");

        Set<String> results = store.list();

        assertThat(results).containsExactly("label");
    }

    @Test
    public void get_deleted_data_returns_null() throws IOException {
        store.put("label", asInputStream("data"));
        store.remove("label");

        InputStream result = store.get("label");

        assertThat(result).isNull();
    }

    @Test
    public void put_overwrites_existing_data() throws IOException {
        store.put("label", asInputStream("old"));
        store.put("label", asInputStream("new"));

        InputStream result = store.get("label");

        assertThat(asString(result)).isEqualTo("new");
    }
}
