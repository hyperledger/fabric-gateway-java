/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.event;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Holds a set of listeners, and allows a function to be applied for each of them to drive notifications.
 * <p>
 * This implementation is thread-safe.
 * </p>
 * @param <T> Listener type.
 */
public final class ListenerSet<T> implements Iterable<T> {
    private static final Log LOG = LogFactory.getLog(ListenerSet.class);

    private final Set<T> listeners = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Add a listener to the set if not already present.
     * @param listener Listener to add.
     * @return The added listener.
     */
    public T add(final T listener) {
        listeners.add(listener);
        return listener;
    }

    /**
     * Remove an existing listener if present.
     * @param listener Listener to remove.
     */
    public void remove(final T listener) {
        listeners.remove(listener);
    }

    /**
     * Remove all listeners.
     */
    public void clear() {
        listeners.clear();
    }

    /**
     * Apply a given function for all listeners. Any exceptions thrown are caught and logged.
     * @param action Function to invoke for each listener.
     */
    @Override
    public void forEach(final Consumer<? super T> action) {
        listeners.forEach(listener -> {
            try {
                action.accept(listener);
            } catch (Exception e) {
                LOG.error("forEach: Exception notifying listener", e);
            }
        });
    }

    @Override
    public Iterator<T> iterator() {
        return listeners.iterator();
    }

    @Override
    public String toString() {
        return listeners.toString();
    }
}
