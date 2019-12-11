/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway;

/**
 * Common behavior shared by all identity implementations.
 */
public interface Identity {
    /**
     * Member services provider to which this identity is associated.
     * @return A member services provider identifier.
     */
    String getMspId();
}
