/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.identity;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.hyperledger.fabric.gateway.spi.WalletStore;

public final class InMemoryWalletStore implements WalletStore {
    private final Map<String, byte[]> store = new HashMap<>();

    @Override
    public void delete(final String label) {
        store.remove(label);
    }

    @Override
    public Optional<InputStream> get(final String label) {
        byte[] data = store.get(label);
        return data != null ? Optional.of(new ByteArrayInputStream(data)) : Optional.empty();
    }

    @Override
    public Set<String> list() {
        return store.keySet();
    }

    @Override
    public void put(final String label, final InputStream data) throws IOException {
        try (InputStream bufferedInput = new BufferedInputStream(data);
             ByteArrayOutputStream byteOutput = new ByteArrayOutputStream()) {
            for (int b; (b = bufferedInput.read()) >= 0; ) { // checkstyle:ignore-line:InnerAssignment
                byteOutput.write(b);
            }
            store.put(label, byteOutput.toByteArray());
        }
    }
}
