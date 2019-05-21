/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.event;

import org.hyperledger.fabric.gateway.spi.Checkpointer;
import org.hyperledger.fabric.sdk.BlockEvent;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Checkpointing of events supplied by a block source. Only blocks with the number expected by the checkpointer are
 * passed to the listener, and the checkpointer block number is updated after events are processed.
 */
public final class CheckpointBlockListenerSession implements ListenerSession {
    private final BlockEventSource blockSource;
    private final Consumer<BlockEvent> listener;
    private final Checkpointer checkpointer;
    private final Consumer<BlockEvent> internalListener;

    public CheckpointBlockListenerSession(BlockEventSource blockSource, Consumer<BlockEvent> listener, Checkpointer checkpointer) {
        this.blockSource = blockSource;
        this.listener = listener;
        this.checkpointer = checkpointer;
        this.internalListener = blockSource.addBlockListener(this::acceptBlock);
    }

    private void acceptBlock(BlockEvent event) {
        final long eventBlockNumber = event.getBlockNumber();
        try {
            synchronized (checkpointer) {
                long checkpointBlockNumber = checkpointer.getBlockNumber();
                if (checkpointBlockNumber == Checkpointer.UNSET_BLOCK_NUMBER) {
                    // Record a starting block in case we don't complete handling and checkpoint below
                    checkpointBlockNumber = eventBlockNumber;
                    checkpointer.setBlockNumber(checkpointBlockNumber);
                }

                if (eventBlockNumber == checkpointBlockNumber) {
                    listener.accept(event); // Process event before checkpointing
                    checkpointer.setBlockNumber(eventBlockNumber + 1);
                }
            }
        } catch (IOException e) {
            // TODO: Log error
        }
    }

    public void close() {
        blockSource.removeBlockListener(internalListener);
        // Don't close checkpointer as this may have been called by the listener on receiving an event, so before
        // the last block number has been checkpointed
    }
}
