/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.identity;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.cert.CertificateException;
import javax.json.Json;
import javax.json.JsonObject;

import org.hyperledger.fabric.gateway.Identities;
import org.hyperledger.fabric.gateway.X509Identity;
import org.hyperledger.fabric.gateway.X509Credentials;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class X509IdentityProviderTest {
    private static final X509Credentials credentials = new X509Credentials();
    private static final String certificatePem = credentials.getCertificatePem();
    private static final String privateKeyPem = credentials.getPrivateKeyPem();
    private static final String mspId = "mspId";
    private static final String typeId = "X.509";

    private final X509IdentityProvider provider = X509IdentityProvider.INSTANCE;

    public static JsonObject jsonV1() {
        return Json.createObjectBuilder()
                .add(IdentityConstants.JSON_VERSION, 1)
                .add(IdentityConstants.JSON_MSP_ID, mspId)
                .add(IdentityConstants.JSON_TYPE, typeId)
                .add("credentials", Json.createObjectBuilder()
                        .add("certificate", certificatePem)
                        .add("privateKey", privateKeyPem)
                        .build())
                .build();
    }

    @Test
    public void get_type() {
        Class<?> result = provider.getType();
        assertThat(result).isEqualTo(X509Identity.class);
    }

    @Test
    public void get_type_ID() {
        String result = provider.getTypeId();
        assertThat(result).isEqualTo(typeId);
    }

    @Test
    public void from_JSON_v1() throws CertificateException, InvalidKeyException, IOException {
        X509Identity result = provider.fromJson(jsonV1());

        assertThat(result).isNotNull();
        assertThat(result.getMspId()).isEqualTo(mspId);
        assertThat(result.getCertificate()).isEqualTo(credentials.getCertificate());
        assertThat(result.getPrivateKey()).isEqualTo(credentials.getPrivateKey());
    }

    @Test
    public void from_JSON_with_invalid_type_ID_throws_IOException() {
        JsonObject jsonData = Json.createObjectBuilder(jsonV1())
                .add("type", "BAD_TYPE")
                .build();

        assertThatThrownBy(() -> provider.fromJson(jsonData))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("BAD_TYPE");
    }

    @Test
    public void from_JSON_with_invalid_version_throws_IOException() {
        JsonObject jsonData = Json.createObjectBuilder(jsonV1())
                .add("version", Integer.MAX_VALUE)
                .build();

        assertThatThrownBy(() -> provider.fromJson(jsonData))
                .isInstanceOf(IOException.class)
                .hasMessageContaining(Integer.toString(Integer.MAX_VALUE));
    }

    @Test
    public void from_JSON_with_missing_fields_throws_IOException() {
        JsonObject jsonData = Json.createObjectBuilder(jsonV1())
                .remove(IdentityConstants.JSON_MSP_ID)
                .build();

        assertThatThrownBy(() -> provider.fromJson(jsonData))
                .isInstanceOf(IOException.class);
    }

    @Test
    public void to_JSON() throws CertificateException, InvalidKeyException, IOException {
        X509Identity expected = Identities.newX509Identity(mspId, credentials.getCertificate(), credentials.getPrivateKey());

        JsonObject jsonData = provider.toJson(expected);
        X509Identity actual = provider.fromJson(jsonData);

        assertThat(actual).isEqualTo(expected);
    }
}
