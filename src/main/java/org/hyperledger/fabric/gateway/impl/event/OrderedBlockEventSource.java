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
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.hyperledger.fabric.gateway.impl.GatewayUtils;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.BlockInfo;

/**
 * Listens to an existing block event source and ensures that its own listeners receive block events in order and
 * without duplicates.
 */
public final class OrderedBlockEventSource implements BlockEventSource {
    private static final Comparator<BlockEvent> EVENT_COMPARATOR = Comparator.comparingLong(BlockEvent::getBlockNumber);

    private final BlockEventSource blockSource;
    private final ListenerSet<Consumer<BlockEvent>> listeners = new ListenerSet<>();
    private final Consumer<BlockEvent> blockListener;

    // Non-threadsafe state synchronized by stateLock
    private final Object stateLock = new Object();
    private long blockNumber;
    private final SortedSet<BlockEvent> queuedEvents = new TreeSet<>(EVENT_COMPARATOR);

    public OrderedBlockEventSource(final BlockEventSource blockSource) {
        this(blockSource, -1);
    }

    public OrderedBlockEventSource(final BlockEventSource blockSource, final long startBlock) {
        this.blockSource = blockSource;
        this.blockListener = blockSource.addBlockListener(this::receivedBlock);
        synchronized (stateLock) {
            blockNumber = startBlock;
        }
    }

    @Override
    public Consumer<BlockEvent> addBlockListener(final Consumer<BlockEvent> listener) {
        return listeners.add(listener);
    }

    @Override
    public void removeBlockListener(final Consumer<BlockEvent> listener) {
        listeners.remove(listener);
    }

    @Override
    public void close() {
        listeners.clear();
        blockSource.removeBlockListener(blockListener);
    }

    private void receivedBlock(final BlockEvent event) {
        synchronized (stateLock) {
            if (isOldBlockNumber(event.getBlockNumber())) {
                return;
            }

            queuedEvents.add(event);
            notifyListeners();
        }
    }

    private boolean isOldBlockNumber(final long eventBlockNumber) {
        return eventBlockNumber < blockNumber;
    }

    private void notifyListeners() {
        for (Iterator<BlockEvent> eventIter = queuedEvents.iterator(); eventIter.hasNext(); ) {
            BlockEvent event = eventIter.next();
            long eventBlockNumber = event.getBlockNumber();

            if (!isNextBlockNumber(eventBlockNumber)) {
                break;
            }

            eventIter.remove();
            blockNumber = eventBlockNumber + 1;
            listeners.forEach(listener -> listener.accept(event));
        }
    }

    private boolean isNextBlockNumber(final long eventBlockNumber) {
        return blockNumber < 0 || blockNumber == eventBlockNumber;
    }

    @Override
    public String toString() {
        final long currentBlockNumber;
        final String queuedBlocks;
        synchronized (stateLock) {
            currentBlockNumber = blockNumber;
            queuedBlocks = queuedEvents.stream()
                    .mapToLong(BlockInfo::getBlockNumber)
                    .mapToObj(Long::toString)
                    .collect(Collectors.joining(", ", "[", "]"));
        }

        return GatewayUtils.toString(this,
                "blockNumber=" + currentBlockNumber,
                "queuedBlocks=" + queuedBlocks);
    }
}
