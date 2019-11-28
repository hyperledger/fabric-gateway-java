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
import org.hyperledger.fabric.gateway.X509Identity;
import org.hyperledger.fabric.gateway.X509Credentials;
import org.hyperledger.fabric.sdk.Enrollment;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

public final class X509IdentityTest {
    private static final String mspId = "mspId";
    private static final X509Credentials credentials = new X509Credentials();

    public static Stream<X509Identity> identityProvider() throws CertificateException, InvalidKeyException, IOException {
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
    public void hashCode_returns_same_value_for_equal_objects(X509Identity identity) {
        int expected = new X509IdentityImpl(mspId, credentials.getCertificate(), credentials.getPrivateKey()).hashCode();
        assertThat(identity.hashCode()).isEqualTo(expected);
    }
}
