/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import org.hyperledger.fabric.gateway.spi.Checkpointer;
import org.hyperledger.fabric.gateway.spi.CheckpointerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FileCheckpointerFactory implements CheckpointerFactory {
    private final Path directory;

    public FileCheckpointerFactory(Path directory) {
        this.directory = directory;
    }

    @Override
    public Checkpointer create(final String networkName, final String checkpointerName) throws IOException {
        validateName(networkName, checkpointerName);

        Path checkpointPath = getCheckpointPath(networkName, checkpointerName);
        Files.createDirectories(checkpointPath.getParent());

        return new FileCheckpointer(checkpointPath);
    }

    private void validateName(String networkName, String checkpointerName) {
        if (networkName == null || networkName.isEmpty()) {
            throw new IllegalArgumentException("Missing network name");
        }
        if (checkpointerName == null || checkpointerName.isEmpty()) {
            throw new IllegalArgumentException("Missing checkpointer name");
        }
    }

    private Path getCheckpointPath(String networkName, String checkpointerName) {
        return directory.resolve(networkName).resolve(checkpointerName);
    }
}
