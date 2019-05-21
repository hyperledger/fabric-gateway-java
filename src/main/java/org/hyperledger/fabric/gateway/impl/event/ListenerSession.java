/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.event;

/**
 * An event listening session that can be closed when no longer required to clean up all resources and registrations
 * associated with the listener. Used to provide a common mechanism for establishing and tearing down both simple and
 * complex event listener implementations.
 */
public interface ListenerSession extends AutoCloseable {
    @Override
    void close();
}
