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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.cloudant.client.api.Database;
import com.cloudant.client.org.lightcouch.DocumentConflictException;
import com.cloudant.client.org.lightcouch.NoDocumentException;
import org.hyperledger.fabric.gateway.impl.GatewayUtils;
import org.hyperledger.fabric.gateway.spi.WalletStore;

public final class CloudantWalletStore implements WalletStore {
    private static final Charset ENCODING = StandardCharsets.UTF_8;

    private static final class IdentityDocument {
        private String _id; // checkstyle:ignore-line:MemberName
        private String _rev; // checkstyle:ignore-line:MemberName
        private String data;

        IdentityDocument(final String id, final InputStream dataIn) throws IOException {
            _id = id;
            setData(dataIn);
        }

        public InputStream getData() {
            return new ByteArrayInputStream(data.getBytes(ENCODING));
        }

        public void setData(final InputStream dataIn) throws IOException {
            try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream()) {
                GatewayUtils.copy(dataIn, byteOut);
                data = new String(byteOut.toByteArray(), ENCODING);
            }
        }
    }

    private final Database database;

    public CloudantWalletStore(final Database database) {
        this.database = database;
    }

    @Override
    public void remove(final String label) {
        IdentityDocument document = getDocument(label);
        if (document != null) {
            try {
                database.remove(document);
            } catch (NoDocumentException e) {
                // Ignore missing document
            }
        }
    }

    @Override
    public InputStream get(final String label) {
        IdentityDocument document = getDocument(label);
        return document != null ? document.getData() : null;
    }

    private IdentityDocument getDocument(final String label) {
        try {
            return database.find(IdentityDocument.class, label);
        } catch (NoDocumentException e) {
            return null;
        }
    }

    @Override
    public Set<String> list() throws IOException {
        List<String> ids = database.getAllDocsRequestBuilder().build().getResponse().getDocIds();
        return new HashSet<>(ids);
    }

    @Override
    public void put(final String label, final InputStream data) throws IOException {
        IdentityDocument document = getDocument(label);
        try {
            if (document != null) {
                document.setData(data);
                database.update(document);
            } else {
                document = new IdentityDocument(label, data);
                database.save(document);
            }
        } catch (DocumentConflictException e) {
            throw new IOException(e);
        }
    }
}
