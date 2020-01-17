/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.identity;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.stream.Stream;

import org.hyperledger.fabric.gateway.Identities;
import org.hyperledger.fabric.gateway.X509Credentials;
import org.hyperledger.fabric.gateway.X509Identity;
import org.hyperledger.fabric.sdk.Enrollment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public final class X509IdentityTest {
    private static final String mspId = "mspId";
    private static final X509Credentials credentials = new X509Credentials();

    public static Stream<X509Identity> identityProvider() throws CertificateException {
        return Stream.of(
                newIdentityFromFromCertificateAndPrivateKey(),
                newIdentityFromEnrollment()
        );
    }

    private static X509Identity newIdentityFromFromCertificateAndPrivateKey() {
        return Identities.newX509Identity(mspId, credentials.getCertificate(), credentials.getPrivateKey());
    }

    private static X509Identity newIdentityFromEnrollment() throws CertificateException {
        Enrollment enrollment = new Enrollment() {
            @Override
            public PrivateKey getKey() {
                return credentials.getPrivateKey();
            }

            @Override
            public String getCert() {
                return credentials.getCertificatePem();
            }
        };

        return Identities.newX509Identity(mspId, enrollment);
    }

    @ParameterizedTest
    @MethodSource("identityProvider")
    void get_MSP_ID(X509Identity identity) {
        String actual = identity.getMspId();
        assertThat(actual).isEqualTo(mspId);
    }

    @ParameterizedTest
    @MethodSource("identityProvider")
    void get_certificate(X509Identity identity) {
        X509Certificate actual = identity.getCertificate();
        assertThat(actual).isEqualTo(credentials.getCertificate());
    }

    @ParameterizedTest
    @MethodSource("identityProvider")
    void get_private_key(X509Identity identity) {
        PrivateKey actual = identity.getPrivateKey();
        assertThat(actual).isEqualTo(credentials.getPrivateKey());
    }

    @ParameterizedTest
    @MethodSource("identityProvider")
    public void equals_returns_true_for_equal_objects(X509Identity identity) {
        Object other = new X509IdentityImpl(mspId, credentials.getCertificate(), credentials.getPrivateKey());
        assertThat(identity).isEqualTo(other);
    }

    @ParameterizedTest
    @MethodSource("identityProvider")
    public void equals_returns_false_for_unequal_objects(X509Identity identity) {
        Object other = new X509IdentityImpl("MISMATCH", credentials.getCertificate(), credentials.getPrivateKey());
        assertThat(identity).isNotEqualTo(other);
    }

    @ParameterizedTest
    @MethodSource("identityProvider")
    public void hashCode_returns_same_value_for_equal_objects(X509Identity identity) throws CertificateException, InvalidKeyException {
        // De/serialize credentials to ensure not just comparing the same objects
        X509Certificate certificate = Identities.readX509Certificate(credentials.getCertificatePem());
        PrivateKey privateKey = Identities.readPrivateKey(credentials.getPrivateKeyPem());

        X509Identity expected = new X509IdentityImpl(mspId, certificate, privateKey);

        assertThat(identity).hasSameHashCodeAs(expected);
    }

    @Test
    public void throws_NullPointerException_if_MSP_ID_is_null() {
        assertThatThrownBy(() -> Identities.newX509Identity(null, credentials.getCertificate(), credentials.getPrivateKey()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void throws_NullPointerException_if_certificate_is_null() {
        assertThatThrownBy(() -> Identities.newX509Identity(mspId, null, credentials.getPrivateKey()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void throws_NullPointerException_if_privateKey_is_null() {
        assertThatThrownBy(() -> Identities.newX509Identity(mspId, credentials.getCertificate(), null))
                .isInstanceOf(NullPointerException.class);
    }
}
