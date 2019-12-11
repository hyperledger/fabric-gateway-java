/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.identity;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.cert.CertificateException;
import javax.json.JsonObject;

import org.hyperledger.fabric.gateway.Identity;
import org.hyperledger.fabric.sdk.HFClient;

public interface IdentityProvider<T extends Identity> {
    Class<T> getType();
    String getTypeId();
    JsonObject toJson(Identity identity);
    T fromJson(JsonObject identityData) throws CertificateException, InvalidKeyException, IOException;
    void setUserContext(HFClient client, Identity identity, String name);
}
