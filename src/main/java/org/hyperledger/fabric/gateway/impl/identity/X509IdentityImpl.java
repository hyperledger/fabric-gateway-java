/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.identity;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Objects;

import org.hyperledger.fabric.gateway.X509Identity;

public final class X509IdentityImpl implements X509Identity {
    private final String mspId;
    private final X509Certificate certificate;
    private final PrivateKey privateKey;

    public X509IdentityImpl(final String mspId, final X509Certificate certificate, final PrivateKey privateKey) {
        if (mspId == null) {
            throw new NullPointerException("mspId must not be null");
        }
        if (certificate == null) {
            throw new NullPointerException("certificate must not be null");
        }
        if (privateKey == null) {
            throw new NullPointerException("privateKey must not be null");
        }

        this.mspId = mspId;
        this.certificate = certificate;
        this.privateKey = privateKey;
    }

    @Override
    public String getMspId() {
        return mspId;
    }

    @Override
    public X509Certificate getCertificate() {
        return certificate;
    }

    @Override
    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof X509IdentityImpl)) {
            return false;
        }

        X509IdentityImpl that = (X509IdentityImpl) other;
        return Objects.equals(this.mspId, that.mspId)
                && Objects.equals(this.certificate, that.certificate)
                && Objects.equals(this.privateKey, that.privateKey);
    }

    @Override
    public int hashCode() {
        // Private key does not have a consistent hash code depending on how it was serialized so don't use that
        return Objects.hash(mspId, certificate);
    }
}
