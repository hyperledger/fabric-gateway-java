/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway;

import java.io.IOException;
import java.nio.file.Path;

import org.hyperledger.fabric.gateway.impl.FileCheckpointer;
import org.hyperledger.fabric.gateway.spi.Checkpointer;

/**
 * Provides static methods used to create instances of default {@link Checkpointer} implementations.
 */
public final class DefaultCheckpointers {
    /**
     * Checkpointer implementation that persists state to a given file. If the file exists, it must contain valid
     * persistent checkpoint state. If the file does not exist, the checkpointer will be created with default initial
     * state, which will start listening from the current block.
     * @param path A file path.
     * @return A checkpointer.
     * @throws IOException if an error occurs creating the checkpointer.
     */
    public static Checkpointer file(Path path) throws IOException {
        return new FileCheckpointer(path);
    }

    private DefaultCheckpointers() { }
}
