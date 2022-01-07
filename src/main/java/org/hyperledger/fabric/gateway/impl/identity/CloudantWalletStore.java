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
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.ibm.cloud.cloudant.v1.Cloudant;
import com.ibm.cloud.cloudant.v1.model.DeleteDocumentOptions;
import com.ibm.cloud.cloudant.v1.model.DocsResultRow;
import com.ibm.cloud.cloudant.v1.model.Document;
import com.ibm.cloud.cloudant.v1.model.GetDocumentOptions;
import com.ibm.cloud.cloudant.v1.model.PostAllDocsOptions;
import com.ibm.cloud.cloudant.v1.model.PutDatabaseOptions;
import com.ibm.cloud.cloudant.v1.model.PutDocumentOptions;
import com.ibm.cloud.sdk.core.http.Response;
import com.ibm.cloud.sdk.core.http.ServiceCall;
import com.ibm.cloud.sdk.core.security.BasicAuthenticator;
import com.ibm.cloud.sdk.core.service.exception.NotFoundException;
import org.hyperledger.fabric.gateway.impl.GatewayUtils;
import org.hyperledger.fabric.gateway.spi.WalletStore;

public final class CloudantWalletStore implements WalletStore {
    private static final Charset ENCODING = StandardCharsets.UTF_8;
    private static final String DATA_FIELD = "data";

    private static final class IdentityDocument {
        private final String id;
        private final String rev;
        private String data;

        IdentityDocument(final String id, final InputStream dataIn) throws IOException {
            this.id = id;
            this.rev = null;
            setData(dataIn);
        }

        IdentityDocument(final Document document) {
            this.id = document.getId();
            this.rev = document.getRev();
            this.data = (String) document.get(DATA_FIELD);
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

        public Document toDocument() {
            Document.Builder builder = new Document.Builder().id(id);
            if (rev != null) {
                builder.rev(rev);
            }
            if (data != null) {
                builder.add(DATA_FIELD, data);
            }

            return builder.build();
        }
    }

    private final Cloudant service;
    private final String databaseName;

    private CloudantWalletStore(final Cloudant service, final String databaseName) {
        this.service = service;
        this.databaseName = databaseName;
    }

    public static CloudantWalletStore newInstance(final URL serverUrl, final String databaseName) {
        Cloudant service = newCloudantService(serverUrl.getUserInfo());
        service.setServiceUrl(serverUrl.toString());

        CloudantWalletStore store = new CloudantWalletStore(service, databaseName);
        try {
            store.createDatabase();
        } catch (IOException e) {
            // Ignore and let failures happen on access to maintain backwards compatibility
        }

        return store;
    }

    private static Cloudant newCloudantService(final String userInfo) {
        if (null == userInfo || userInfo.isEmpty()) {
            return Cloudant.newInstance();
        }

        final BasicAuthenticator.Builder authBuilder = new BasicAuthenticator.Builder();

        int separatorIndex = userInfo.indexOf(':');
        if (separatorIndex >= 0) {
            String user = userInfo.substring(0, separatorIndex);
            String password = userInfo.substring(separatorIndex + 1);
            authBuilder.username(user).password(password);
        } else {
            authBuilder.username(userInfo);
        }

        return new Cloudant(Cloudant.DEFAULT_SERVICE_NAME, authBuilder.build());
    }


    private void createDatabase() throws IOException {
        boolean exists = execute(Cloudant::getAllDbs)
                .getResult()
                .contains(databaseName);

        if (!exists) {
            PutDatabaseOptions options = new PutDatabaseOptions.Builder()
                    .db(databaseName)
                    .build();
            execute(service -> service.putDatabase(options));
        }
    }

    @Override
    public void remove(final String label) throws IOException {
        IdentityDocument document = getDocument(label);
        if (null == document) {
            return;
        }

        DeleteDocumentOptions options = new DeleteDocumentOptions.Builder()
                .db(databaseName)
                .docId(document.id)
                .rev(document.rev)
                .build();
        execute(service -> service.deleteDocument(options));
    }

    private IdentityDocument getDocument(final String label) throws IOException {
        GetDocumentOptions options = new GetDocumentOptions.Builder()
                .db(databaseName)
                .docId(label)
                .build();
        try {
            Document document = service.getDocument(options)
                    .execute()
                    .getResult();
            return new IdentityDocument(document);
        } catch (NotFoundException e) {
            return null;
        } catch (RuntimeException e) {
            throw new IOException(e);
        }
    }

    private <T> Response<T> execute(final Function<Cloudant, ServiceCall<T>> call) throws IOException {
        try {
            return call.apply(service).execute();
        } catch (RuntimeException e) {
            throw new IOException(e);
        }
    }

    @Override
    public InputStream get(final String label) throws IOException {
        IdentityDocument document = getDocument(label);
        return document != null ? document.getData() : null;
    }

    @Override
    public Set<String> list() throws IOException {
        PostAllDocsOptions options = new PostAllDocsOptions.Builder()
                .db(databaseName)
                .build();
        return execute(service -> service.postAllDocs(options))
                    .getResult()
                    .getRows()
                    .stream()
                    .map(DocsResultRow::getId)
                    .collect(Collectors.toSet());
    }

    @Override
    public void put(final String label, final InputStream data) throws IOException {
        IdentityDocument document = getDocument(label);
        if (document != null) {
            document.setData(data);
        } else {
            document = new IdentityDocument(label, data);
        }

        PutDocumentOptions options = new PutDocumentOptions.Builder()
                .db(databaseName)
                .docId(document.id)
                .document(document.toDocument())
                .build();
        execute(service -> service.putDocument(options))
                .getResult();
    }
}
