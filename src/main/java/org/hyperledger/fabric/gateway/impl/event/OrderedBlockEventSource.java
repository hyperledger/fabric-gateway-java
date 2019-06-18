/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.event;

import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.BlockInfo;

/**
 * Listens to an existing block event source and ensures that its own listeners receive block events in order and
 * without duplicates.
 */
public final class OrderedBlockEventSource implements BlockEventSource {
    private static final Comparator<BlockEvent> eventComparator = Comparator.comparingLong(BlockEvent::getBlockNumber);

    private final BlockEventSource blockSource;
    private final ListenerSet<Consumer<BlockEvent>> listeners = new ListenerSet<>();
    private final Consumer<BlockEvent> blockListener;
    private final AtomicLong blockNumber;
    private final SortedSet<BlockEvent> queuedEvents = new TreeSet<>(eventComparator);
    private final Object eventHandlingLock = new Object();

    public OrderedBlockEventSource(BlockEventSource blockSource) {
        this(blockSource, -1);
    }

    public OrderedBlockEventSource(BlockEventSource blockSource, long startBlock) {
        this.blockSource = blockSource;
        this.blockListener = blockSource.addBlockListener(this::receivedBlock);
        blockNumber = new AtomicLong(startBlock);
    }

    @Override
    public Consumer<BlockEvent> addBlockListener(Consumer<BlockEvent> listener) {
        return listeners.add(listener);
    }

    @Override
    public void removeBlockListener(Consumer<BlockEvent> listener) {
        listeners.remove(listener);
    }

    @Override
    public void close() {
        listeners.clear();
        blockSource.removeBlockListener(blockListener);
    }

    private void receivedBlock(BlockEvent event) {
        synchronized (eventHandlingLock) {
            if (isOldBlockNumber(event.getBlockNumber())) {
                return;
            }

            queuedEvents.add(event);
            notifyListeners();
        }
    }

    private boolean isOldBlockNumber(long eventBlockNumber) {
        return eventBlockNumber < blockNumber.get();
    }

    private void notifyListeners() {
        for (Iterator<BlockEvent> eventIter = queuedEvents.iterator(); eventIter.hasNext(); ) {
            BlockEvent event = eventIter.next();
            long eventBlockNumber = event.getBlockNumber();

            if (!isNextBlockNumber(eventBlockNumber)) {
                break;
            }

            eventIter.remove();
            blockNumber.set(eventBlockNumber + 1);
            listeners.forEach(listener -> listener.accept(event));
        }
    }

    private boolean isNextBlockNumber(long eventBlockNumber) {
        final long nextBlockNumber = blockNumber.get();
        return nextBlockNumber < 0 || nextBlockNumber == eventBlockNumber;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '@' + System.identityHashCode(this) +
                "(blockNumber=" + blockNumber.get() +
                ", queuedBlocks=" + queuedEvents.stream()
                        .mapToLong(BlockInfo::getBlockNumber)
                        .mapToObj(Long::toString)
                        .collect(Collectors.joining(", ", "[", "]")) + ')';
    }
}