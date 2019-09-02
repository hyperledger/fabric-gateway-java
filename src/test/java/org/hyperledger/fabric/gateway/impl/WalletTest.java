/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import org.hyperledger.fabric.gateway.TestUtils;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallet.Identity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class WalletTest {
    Wallet wallet = null;
    Identity identity1 = null;
    Identity identity2 = null;
    Path certPath = Paths.get("src", "test", "java", "org", "hyperledger", "fabric", "gateway");
    static PrivateKey pk;
    static String certificate;

    @BeforeAll
    public static void init() throws Exception {
        Enrollment enrollment = TestUtils.getInstance().newEnrollment();
        pk = enrollment.getPrivateKey();
        certificate = enrollment.getCertificate();
    }

    @BeforeEach
    public void setup() throws Exception {
        identity1 = new WalletIdentity("org1msp", certificate, pk);
        identity2 = new WalletIdentity("org2msp", certificate, pk);
    }

    @Test
    public void testPut() throws Exception {
        wallet.put("label1", identity1);
        assertThat(wallet.exists("label1")).isTrue();
    }

    @Test
    public void testGet() throws Exception {
        wallet.put("label1", identity1);
        Identity id1 = wallet.get("label1");
        assertThat(identity1.getMspId()).isEqualTo(id1.getMspId());
        assertThat(identity1.getCertificate()).isEqualToNormalizingNewlines(id1.getCertificate());
        Identity id2 = wallet.get("label2");
        assertThat(id2).isNull();
    }

    @Test
    public void testPutOverwrite() throws Exception {
        wallet.put("label1", identity1);
        assertThat(wallet.exists("label1")).isTrue();
        wallet.put("label1", identity2);
        Identity id1 = wallet.get("label1");
        assertThat(identity2.getMspId()).isEqualTo(id1.getMspId());
    }

    @Test
    public void testGetAllLabels() throws Exception {
        wallet.put("label1", identity1);
        Set<String> ids = wallet.getAllLabels();
        assertThat(ids).containsExactly("label1");
        wallet.put("label2", identity2);
        ids = wallet.getAllLabels();
        assertThat(ids).containsExactlyInAnyOrder("label1", "label2");
    }

    @Test
    public void testRemove() throws Exception {
        wallet.put("label1", identity1);
        wallet.put("label2", identity2);
        Set<String> ids = wallet.getAllLabels();
        assertThat(ids).containsExactlyInAnyOrder("label1", "label2");
        wallet.remove("label2");
        ids = wallet.getAllLabels();
        assertThat(ids).containsExactly("label1");
    }

    @Test
    public void testRemoveTwice() throws Exception {
        wallet.put("label1", identity1);
        wallet.put("label2", identity2);
        wallet.remove("label2");
        Set<String> ids = wallet.getAllLabels();
        assertThat(ids).containsExactly("label1");
        // remove again - should silently ignore
        wallet.remove("label2");
        ids = wallet.getAllLabels();
        assertThat(ids).containsExactly("label1");
    }
}
