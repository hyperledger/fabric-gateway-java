/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.identity;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import org.hyperledger.fabric.gateway.impl.GatewayUtils;
import org.hyperledger.fabric.gateway.spi.WalletStore;

public final class FileSystemWalletStore implements WalletStore {
    private static final String DATA_FILE_EXTENTION = ".id";
    private final Path storePath;

    public FileSystemWalletStore(final Path storePath) throws IOException {
        this.storePath = storePath;

        if (!Files.isDirectory(storePath)) {
            Files.createDirectories(storePath);
        }
    }

    @Override
    public void remove(final String label) throws IOException {
        Path dataPath = getPathForLabel(label);
        Files.deleteIfExists(dataPath);
    }

    private Path getPathForLabel(final String label) {
        return storePath.resolve(label + DATA_FILE_EXTENTION);
    }

    @Override
    public InputStream get(final String label) {
        try {
            return Files.newInputStream(getPathForLabel(label));
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public Set<String> list() throws IOException {
        return Files.list(storePath)
                .map(Path::getFileName)
                .map(Path::toString)
                .filter(this::isDataFile)
                .map(this::getLabelForFile)
                .collect(Collectors.toSet());
    }

    private boolean isDataFile(final String fileName) {
        return fileName.endsWith(DATA_FILE_EXTENTION);
    }

    private String getLabelForFile(final String fileName) {
        return fileName.substring(0, fileName.length() - DATA_FILE_EXTENTION.length());
    }

    @Override
    public void put(final String label, final InputStream data) throws IOException {
        Path dataPath = getPathForLabel(label);
        try (OutputStream fileOut = Files.newOutputStream(dataPath);
             BufferedOutputStream bufferedOut = new BufferedOutputStream(fileOut)) {
            GatewayUtils.copy(data, bufferedOut);
            bufferedOut.flush();
        }
    }
}
