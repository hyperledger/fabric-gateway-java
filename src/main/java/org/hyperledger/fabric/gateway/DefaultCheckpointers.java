/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway;

import org.hyperledger.fabric.gateway.impl.FileCheckpointer;
import org.hyperledger.fabric.gateway.impl.InMemoryCheckpointer;
import org.hyperledger.fabric.gateway.spi.Checkpointer;

import java.io.IOException;
import java.nio.file.Path;

public final class DefaultCheckpointers {
    /**
     * Checkpointer implementation that persists state to a given file. If the file exists, it must contain valid
     * persistent checkpoint state. If the file does not exist, the checkpointer will be created with default initial
     * state, which will start listening from the current block.
     * @param path A file path.
     * @return A checkpointer.
     * @throws IOException
     */
    public static Checkpointer newFileCheckpointer(Path path) throws IOException {
        return new FileCheckpointer(path);
    }

    /**
     * Transient in-memory checkpointer implementation with no persistent storage. Can be used for event replay.
     * @return A checkpointer.
     */
    public static Checkpointer newInMemoryCheckpointer() {
        return new InMemoryCheckpointer();
    }

    private DefaultCheckpointers() { }
}
