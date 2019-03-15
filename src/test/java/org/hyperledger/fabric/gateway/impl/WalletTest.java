/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallet.Identity;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.util.Set;

public abstract class WalletTest {
  Wallet wallet = null;
  Identity identity1 = null;
  Identity identity2 = null;
  Path certPath = Paths.get("src", "test", "java", "org", "hyperledger", "fabric", "gateway");
  static PrivateKey pk;
  static String certificate;

  @BeforeAll
  public static void init() throws Exception {
	  Enrollment enrollment = Enrollment.createTestEnrollment();
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
    Assert.assertTrue(wallet.exists("label1"));
  }

  @Test
  public void testGet() throws Exception {
    wallet.put("label1", identity1);
    Identity id1 = wallet.get("label1");
    Assert.assertEquals(identity1.getMspId(), id1.getMspId());
    Assert.assertEquals(identity1.getCertificate(), id1.getCertificate());
    Identity id2 = wallet.get("label2");
    Assert.assertNull(id2);
  }

  @Test
  public void testPutOverwrite() throws Exception {
    wallet.put("label1", identity1);
    Assert.assertTrue(wallet.exists("label1"));
    wallet.put("label1", identity2);
    Identity id1 = wallet.get("label1");
    Assert.assertEquals(identity2.getMspId(), id1.getMspId());
  }

  @Test
  public void testGetAllLabels() throws Exception {
    wallet.put("label1", identity1);
    Set<String> ids = wallet.getAllLabels();
    Assert.assertEquals(ids.size(), 1);
    Assert.assertTrue(ids.contains("label1"));
    wallet.put("label2", identity2);
    ids = wallet.getAllLabels();
    Assert.assertEquals(ids.size(), 2);
    Assert.assertTrue(ids.contains("label1"));
    Assert.assertTrue(ids.contains("label2"));
  }

  @Test
  public void testRemove() throws Exception {
    wallet.put("label1", identity1);
    wallet.put("label2", identity2);
    Set<String> ids = wallet.getAllLabels();
    Assert.assertEquals(ids.size(), 2);
    Assert.assertTrue(ids.contains("label1"));
    Assert.assertTrue(ids.contains("label2"));
    wallet.remove("label2");
    ids = wallet.getAllLabels();
    Assert.assertEquals(ids.size(), 1);
    Assert.assertTrue(ids.contains("label1"));
  }

  @Test
  public void testRemoveTwice() throws Exception {
    wallet.put("label1", identity1);
    wallet.put("label2", identity2);
    wallet.remove("label2");
    Set<String> ids = wallet.getAllLabels();
    Assert.assertEquals(ids.size(), 1);
    Assert.assertTrue(ids.contains("label1"));
    // remove again - should silently ignore
    wallet.remove("label2");
    ids = wallet.getAllLabels();
    Assert.assertEquals(ids.size(), 1);
    Assert.assertTrue(ids.contains("label1"));
  }
}
