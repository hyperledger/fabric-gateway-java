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
import java.security.InvalidKeyException;
import java.security.cert.CertificateException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonWriter;

import org.hyperledger.fabric.gateway.Identity;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.spi.WalletStore;

public final class WalletImpl implements Wallet {
    private final WalletStore store;
    private final Map<String, IdentityProvider<?>> providers = Stream.of(X509IdentityProvider.INSTANCE)
            .collect(Collectors.toMap(X509IdentityProvider::getTypeId, provider -> provider));

    public WalletImpl(final WalletStore store) {
        this.store = store;
    }

    @Override
    public void put(final String label, final Identity identity) throws IOException {
        try (InputStream byteInStream = serializeIdentity(identity)) {
            store.put(label, byteInStream);
        }
    }

    private InputStream serializeIdentity(final Identity identity) {
        IdentityProvider<?> provider = getProvider(identity);
        JsonObject identityJson = provider.toJson(identity);
        return serializeJson(identityJson);
    }

    private IdentityProvider<?> getProvider(final Identity identity) {
        Class<? extends Identity> identityType = identity.getClass();
        return providers.values().stream()
                .filter(provider -> provider.getType().isAssignableFrom(identityType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No provider for identity type: " + identityType.getName()));
    }

    private InputStream serializeJson(final JsonObject identityJson) {
        ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
        try (JsonWriter jsonWriter = Json.createWriter(byteOutStream)) {
            jsonWriter.writeObject(identityJson);
        }
        return new ByteArrayInputStream(byteOutStream.toByteArray());
    }

    @Override
    public Identity get(final String label) throws IOException {
        try (InputStream identityData = store.get(label)) {
            if (identityData == null) {
                return null;
            }

            try {
                return deserializeIdentity(identityData);
            } catch (RuntimeException e) {
                throw new IOException(e);
            }
        }
    }

    private Identity deserializeIdentity(final InputStream identityData) throws IOException {
        JsonObject identityJson = Json.createReader(identityData).readObject();
        String type = identityJson.getString(IdentityConstants.JSON_TYPE);
        IdentityProvider<?> provider = getProvider(type);
        try {
            return provider.fromJson(identityJson);
        } catch (CertificateException | InvalidKeyException e) {
            throw new IOException(e);
        }
    }

    private IdentityProvider<?> getProvider(final String typeId) {
        final IdentityProvider<?> result = providers.get(typeId);
        if (result == null) {
            throw new IllegalArgumentException("No provider for identity type ID: " + typeId);
        }
        return result;
    }

    @Override
    public Set<String> list() throws IOException {
        return store.list();
    }

    @Override
    public void remove(final String label) throws IOException {
        store.remove(label);
    }
}
