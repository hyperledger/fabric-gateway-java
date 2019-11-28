/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * Identity comprising an X.509 certificate and associated private key. Instances are created using
 * {@link Identities#newX509Identity(String, X509Certificate, PrivateKey)}.
 */
public interface X509Identity extends Identity {
    /**
     * Get the certificate for this identity.
     * @return An X.509 certificate.
     */
    X509Certificate getCertificate();

    /**
     * Get the private key for this identity.
     * @return A private key.
     */
    PrivateKey getPrivateKey();
}
