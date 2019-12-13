/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMException;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator;
import org.bouncycastle.util.io.pem.PemGenerationException;
import org.bouncycastle.util.io.pem.PemObject;
import org.hyperledger.fabric.gateway.impl.identity.X509IdentityImpl;
import org.hyperledger.fabric.sdk.Enrollment;

/**
 * This class consists exclusively of static methods used to create and operate on identity information.
 */
public final class Identities {
    /**
     * Create a new identity using X.509 credentials.
     * @param mspId Member Services Provider identifier for the organization to which this identity belongs.
     * @param certificate An X.509 certificate.
     * @param privateKey Private key.
     * @return An identity.
     * @throws NullPointerException if any of the arguments are null.
     */
    public static X509Identity newX509Identity(final String mspId, final X509Certificate certificate, final PrivateKey privateKey) {
        return new X509IdentityImpl(mspId, certificate, privateKey);
    }

    /**
     * Create a new X.509 identity from an enrollment returned from a Certificate Authority.
     * @param mspId Member Services Provider identifier.
     * @param enrollment Identity credentials.
     * @return An identity.
     * @throws CertificateException if the certificate is invalid.
     * @throws NullPointerException if any of the arguments are null.
     */
    public static X509Identity newX509Identity(final String mspId, final Enrollment enrollment) throws CertificateException {
        return newX509Identity(mspId, readX509Certificate(enrollment.getCert()), enrollment.getKey());
    }

    /**
     * Read a PEM format X.509 certificate.
     * @param pem PEM data.
     * @return An X.509 certificate.
     * @throws CertificateException if the data is not valid X.509 certificate PEM.
     */
    public static X509Certificate readX509Certificate(final String pem) throws CertificateException {
        try {
            return readX509Certificate(new StringReader(pem));
        } catch (IOException e) {
            // Should not happen with StringReader
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Read a PEM format X.509 certificate.
     * @param pemReader Reader of PEM data.
     * @return An X.509 certificate.
     * @throws IOException if an error occurs reading data.
     * @throws CertificateException if the data is not valid X.509 certificate PEM.
     */
    public static X509Certificate readX509Certificate(final Reader pemReader) throws IOException, CertificateException {
        try {
            Object pemObject = readPemObject(pemReader);
            X509CertificateHolder certificateHolder = asX509CertificateHolder(pemObject);
            return new JcaX509CertificateConverter().getCertificate(certificateHolder);
        } catch (PEMException e) {
            throw new CertificateException(e);
        }
    }

    private static Object readPemObject(final Reader reader) throws IOException {
        try (PEMParser parser = new PEMParser(reader)) {
            final Object result = parser.readObject(); // throws PEMException on parse error
            if (result == null) {
                throw new PEMException("Invalid PEM content");
            }
            return result;
        }
    }

    private static X509CertificateHolder asX509CertificateHolder(final Object pemObject) throws CertificateException {
        if (pemObject instanceof X509CertificateHolder) {
            return (X509CertificateHolder) pemObject;
        } else {
            throw new CertificateException("Unexpected PEM content type: " + pemObject.getClass().getSimpleName());
        }
    }

    /**
     * Read a PEM format private key.
     * @param pem PEM data.
     * @return An X.509 certificate.
     * @throws InvalidKeyException if the data is not valid private key PEM.
     */
    public static PrivateKey readPrivateKey(final String pem) throws InvalidKeyException {
        try {
            return readPrivateKey(new StringReader(pem));
        } catch (IOException e) {
            // Should not happen with StringReader
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Read a PEM format private key.
     * @param pemReader Reader of PEM data.
     * @return A private key.
     * @throws IOException if an error occurs reading data.
     * @throws InvalidKeyException if the data is not valid private key PEM.
     */
    public static PrivateKey readPrivateKey(final Reader pemReader) throws IOException, InvalidKeyException {
        try {
            Object pemObject = readPemObject(pemReader);
            PrivateKeyInfo privateKeyInfo = asPrivateKeyInfo(pemObject);
            return new JcaPEMKeyConverter().getPrivateKey(privateKeyInfo);
        } catch (PEMException e) {
            throw new InvalidKeyException(e);
        }
    }

    private static PrivateKeyInfo asPrivateKeyInfo(final Object pemObject) throws InvalidKeyException {
        PrivateKeyInfo privateKeyInfo;
        if (pemObject instanceof PEMKeyPair) {
            privateKeyInfo = ((PEMKeyPair) pemObject).getPrivateKeyInfo();
        } else if (pemObject instanceof PrivateKeyInfo) {
            privateKeyInfo = (PrivateKeyInfo) pemObject;
        } else {
            throw new InvalidKeyException("Unexpected PEM content type: " + pemObject.getClass().getSimpleName());
        }
        return privateKeyInfo;
    }

    /**
     * Converts the argument to a PEM format string.
     * @param certificate A certificate.
     * @return A PEM format string.
     */
    public static String toPemString(final Certificate certificate) {
        return asPemString(certificate);
    }

    private static String asPemString(final Object obj) {
        StringWriter stringWriter = new StringWriter();
        try (JcaPEMWriter pemWriter = new JcaPEMWriter(stringWriter)) {
            pemWriter.writeObject(obj);
            pemWriter.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return stringWriter.toString();
    }

    /**
     * Converts the argument to a PKCS #8 PEM format string.
     * @param privateKey A private key.
     * @return A PEM format string.
     * @throws IllegalArgumentException if the argument can not be represented in PKCS #8 PEM format.
     */
    public static String toPemString(final PrivateKey privateKey) {
        try {
            PemObject pkcs8PrivateKey = new JcaPKCS8Generator(privateKey, null).generate();
            return asPemString(pkcs8PrivateKey);
        } catch (PemGenerationException e) {
            throw new IllegalArgumentException(e);
        }
    }

    // Private constructor to prevent instantiation
    private Identities() { }
}
