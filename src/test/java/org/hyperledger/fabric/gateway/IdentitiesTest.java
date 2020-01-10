/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class IdentitiesTest {
    private static final TestUtils testUtils = TestUtils.getInstance();

    private final X509Credentials credentials = new X509Credentials();
    private final String x509CertificatePem = "-----BEGIN CERTIFICATE-----\n" +
            "MIICGDCCAb+gAwIBAgIQHWBLQRSL/SxAckSUBCAceDAKBggqhkjOPQQDAjBzMQsw\n" +
            "CQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMNU2FuIEZy\n" +
            "YW5jaXNjbzEZMBcGA1UEChMQb3JnMS5leGFtcGxlLmNvbTEcMBoGA1UEAxMTY2Eu\n" +
            "b3JnMS5leGFtcGxlLmNvbTAeFw0xOTEyMTAxMzA1MDBaFw0yOTEyMDcxMzA1MDBa\n" +
            "MFsxCzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpDYWxpZm9ybmlhMRYwFAYDVQQHEw1T\n" +
            "YW4gRnJhbmNpc2NvMR8wHQYDVQQDDBZVc2VyMUBvcmcxLmV4YW1wbGUuY29tMFkw\n" +
            "EwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEv4CMIDkoFaqG0LEj5e1rHdHS5fdaUcLo\n" +
            "5QPMEPp9xlF9coWfAZ8kVwzDhw+G4dDnZDNYrMoZK1XCpGMcsXsNcqNNMEswDgYD\n" +
            "VR0PAQH/BAQDAgeAMAwGA1UdEwEB/wQCMAAwKwYDVR0jBCQwIoAgwIX0Gf47FIot\n" +
            "fjItLkLWW7jHtTfOqKIDU6lUNl4rYwEwCgYIKoZIzj0EAwIDRwAwRAIgX7lWMVFu\n" +
            "O6R7m7rxRD/A8hmEVcogX6x1kt7NvWH0OfgCIHpKlOFXN50hrMirci4scErbc/ra\n" +
            "G8OCh+bs1rqfv9cM\n" +
            "-----END CERTIFICATE-----";
    private final String pkcs8PrivateKeyPem = "-----BEGIN PRIVATE KEY-----\n" +
            "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQg8yzkTu0ilOAwJZgj\n" +
            "fU/MO5V532NgyJEB7QW6KKsrTwGhRANCAAS/gIwgOSgVqobQsSPl7Wsd0dLl91pR\n" +
            "wujlA8wQ+n3GUX1yhZ8BnyRXDMOHD4bh0OdkM1isyhkrVcKkYxyxew1y\n" +
            "-----END PRIVATE KEY-----";

    @Test
    public void certificate_read_error_throws_IOException() {
        String failMessage = "read failure";
        Reader reader = testUtils.newFailingReader(failMessage);

        assertThatThrownBy(() -> Identities.readX509Certificate(reader))
                .isInstanceOf(IOException.class)
                .hasMessage(failMessage);
    }

    @Test
    public void bad_certificate_PEM_throws_CertificateException() {
        assertThatThrownBy(() -> Identities.readX509Certificate("Invalid PEM"))
                .isInstanceOf(CertificateException.class);
    }

    @Test
    public void bad_certificate_throws_CertificateException() {
        String pem = "-----BEGIN CERTIFICATE-----\n" +
                Base64.getEncoder().encodeToString("Invalid certificate".getBytes(StandardCharsets.UTF_8)) + "\n" +
                "-----END CERTIFICATE-----";

        assertThatThrownBy(() -> Identities.readX509Certificate(pem))
                .isInstanceOf(CertificateException.class);
    }

    @Test
    public void private_key_read_error_throws_IOException() {
        String failMessage = "read failure";
        Reader reader = testUtils.newFailingReader(failMessage);

        assertThatThrownBy(() -> Identities.readPrivateKey(reader))
                .isInstanceOf(IOException.class)
                .hasMessage(failMessage);
    }

    @Test
    public void bad_private_key_PEM_throws_InvalidKeyException() {
        assertThatThrownBy(() -> Identities.readPrivateKey("Invalid PEM"))
                .isInstanceOf(InvalidKeyException.class);
    }

    @Test
    public void bad_private_key_throws_InvalidKeyException() {
        String pem = "-----BEGIN PRIVATE KEY-----\n" +
                Base64.getEncoder().encodeToString("Invalid private key".getBytes(StandardCharsets.UTF_8)) + "\n" +
                "-----END PRIVATE KEY-----";

        assertThatThrownBy(() -> Identities.readPrivateKey(pem))
                .isInstanceOf(InvalidKeyException.class);
    }

    @Test
    public void read_and_write_X509_certificate_PEM() throws CertificateException {
        Certificate certificate = Identities.readX509Certificate(x509CertificatePem);
        String result = Identities.toPemString(certificate);

        assertThat(result).isEqualToIgnoringNewLines(x509CertificatePem);
    }

    @Test
    public void read_and_write_PKCS8_private_key_PEM() throws InvalidKeyException {
        PrivateKey privateKey = Identities.readPrivateKey(pkcs8PrivateKeyPem);
        String result = Identities.toPemString(privateKey);

        assertThat(result).isEqualToIgnoringNewLines(pkcs8PrivateKeyPem);
    }

    @Test
    public void write_and_read_X509_certificate() throws CertificateException {
        Certificate expected = credentials.getCertificate();
        String pem = Identities.toPemString(expected);
        Certificate actual = Identities.readX509Certificate(pem);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void write_and_read_private_key() throws InvalidKeyException {
        PrivateKey expected = credentials.getPrivateKey();
        String pem = Identities.toPemString(expected);
        PrivateKey actual = Identities.readPrivateKey(pem);

        assertThat(actual).isEqualTo(expected);
    }
}
