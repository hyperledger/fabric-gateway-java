/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

import org.hyperledger.fabric.gateway.impl.FileCheckpointer;
import org.hyperledger.fabric.gateway.impl.InMemoryCheckpointer;
import org.hyperledger.fabric.gateway.spi.Checkpointer;

public final class DefaultCheckpointers {
    /**
     * Checkpointer implementation that persists state to a given file. If the file exists, it must contain valid
     * persistent checkpoint state. If the file does not exist, the checkpointer will be created with default initial
     * state, which will start listening from the current block.
     * @param path A file path.
     * @return A checkpointer.
     * @throws IOException
     */
    public static Checkpointer file(Path path) throws IOException {
        return new FileCheckpointer(path);
    }

    /**
     * Transient in-memory checkpointer implementation with no persistent storage. Can be used for event replay in
     * combination with listener methods such as {@link Network#addBlockListener(Checkpointer, Consumer)}. For example,
     * to replay from block 1:
     * <pre>{@code
     * network.addBlockListener(Checkpointers.replay(1), blockListener);
     * }</pre>
     * @param startBlockNumber Initial block number.
     * @return A checkpointer.
     */
    public static Checkpointer replay(long startBlockNumber) {
        return new InMemoryCheckpointer(startBlockNumber);
    }

    private DefaultCheckpointers() { }
}
