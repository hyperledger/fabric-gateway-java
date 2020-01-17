/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.identity;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.json.Json;
import javax.json.JsonObject;

import org.hyperledger.fabric.gateway.GatewayRuntimeException;
import org.hyperledger.fabric.gateway.Identities;
import org.hyperledger.fabric.gateway.Identity;
import org.hyperledger.fabric.gateway.X509Identity;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.identity.X509Enrollment;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric.sdk.security.CryptoSuiteFactory;

public enum X509IdentityProvider implements IdentityProvider<X509Identity> {
    INSTANCE;

    private static final String TYPE_ID = "X.509";
    private static final String JSON_CREDENTIALS = "credentials";
    private static final String JSON_CERTIFICATE = "certificate";
    private static final String JSON_PRIVATE_KEY = "privateKey";

    @Override
    public Class<X509Identity> getType() {
        return X509Identity.class;
    }

    @Override
    public String getTypeId() {
        return TYPE_ID;
    }

    @Override
    public JsonObject toJson(final Identity identity) {
        X509Identity x509identity = (X509Identity) identity;

        String certificatePem = Identities.toPemString(x509identity.getCertificate());
        String privateKeyPem = Identities.toPemString(x509identity.getPrivateKey());

        return Json.createObjectBuilder()
                .add(IdentityConstants.JSON_VERSION, 1)
                .add(IdentityConstants.JSON_MSP_ID, x509identity.getMspId())
                .add(IdentityConstants.JSON_TYPE, TYPE_ID)
                .add(JSON_CREDENTIALS, Json.createObjectBuilder()
                        .add(JSON_CERTIFICATE, certificatePem)
                        .add(JSON_PRIVATE_KEY, privateKeyPem))
                .build();
    }

    @Override
    public X509Identity fromJson(final JsonObject identityData) throws CertificateException, InvalidKeyException, IOException {
        try {
            return deserializeIdentity(identityData);
        } catch (RuntimeException e) {
            // May receive a runtime exception if JSON data is not of the expected format
            throw new IOException(e);
        }
    }

    private X509Identity deserializeIdentity(final JsonObject identityData) throws IOException, CertificateException, InvalidKeyException {
        final String type = identityData.getString(IdentityConstants.JSON_TYPE);
        if (!TYPE_ID.equals(type)) {
            throw new IOException("Bad type for provider: " + type);
        }

        final int version = identityData.getInt(IdentityConstants.JSON_VERSION);
        switch (version) {
            case 1:
                return newIdentity(identityData);
            default:
                throw new IOException("Unsupported identity data version: " + version);
        }
    }

    private X509Identity newIdentity(final JsonObject identityData) throws CertificateException, InvalidKeyException {
        String mspId = identityData.getString(IdentityConstants.JSON_MSP_ID);

        JsonObject credentials = identityData.getJsonObject(JSON_CREDENTIALS);
        String certificatePem = credentials.getString(JSON_CERTIFICATE);
        String privateKeyPem = credentials.getString(JSON_PRIVATE_KEY);

        X509Certificate certificate = Identities.readX509Certificate(certificatePem);
        PrivateKey privateKey = Identities.readPrivateKey(privateKeyPem);
        return Identities.newX509Identity(mspId, certificate, privateKey);
    }

    @Override
    public void setUserContext(final HFClient client, final Identity identity, final String name) {
        X509Identity x509Identity = (X509Identity) identity;

        String certificatePem = Identities.toPemString(x509Identity.getCertificate());
        Enrollment enrollment = new X509Enrollment(x509Identity.getPrivateKey(), certificatePem);
        User user = new GatewayUser(name, x509Identity.getMspId(), enrollment);

        try {
            CryptoSuite cryptoSuite = CryptoSuiteFactory.getDefault().getCryptoSuite();
            client.setCryptoSuite(cryptoSuite);
            client.setUserContext(user);
        } catch (ClassNotFoundException | CryptoException | IllegalAccessException | NoSuchMethodException
                | InstantiationException | InvalidArgumentException | InvocationTargetException e) {
            throw new GatewayRuntimeException("Failed to configure user context", e);
        }
    }
}
