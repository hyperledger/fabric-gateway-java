/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import org.hyperledger.fabric.gateway.spi.Checkpointer;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public final class FileCheckpointer implements Checkpointer {
    private static final Set<OpenOption> OPEN_OPTIONS = Collections.unmodifiableSet(EnumSet.of(
            StandardOpenOption.CREATE,
            StandardOpenOption.READ,
            StandardOpenOption.WRITE
    ));
    private static final int VERSION = 1;
    private static final String CONFIG_KEY_VERSION = "version";
    private static final String CONFIG_KEY_BLOCK = "block";
    private static final String CONFIG_KEY_TRANSACTIONS = "transactions";

    private final Path filePath;
    private final FileChannel fileChannel;
    private final Reader fileReader;
    private final Writer fileWriter;
    private long blockNumber = Checkpointer.UNSET_BLOCK_NUMBER;
    private Set<String> transactionIds = new HashSet<>();

    public FileCheckpointer(Path checkpointFile) throws IOException {
        boolean isFileAlreadyPresent = Files.exists(checkpointFile);

        filePath = checkpointFile;
        fileChannel = FileChannel.open(filePath, OPEN_OPTIONS);
        lockFile();

        CharsetDecoder utf8Decoder = StandardCharsets.UTF_8.newDecoder();
        fileReader = Channels.newReader(fileChannel, utf8Decoder, -1);

        CharsetEncoder utf8Encoder = StandardCharsets.UTF_8.newEncoder();
        fileWriter = Channels.newWriter(fileChannel, utf8Encoder, -1);

        if (isFileAlreadyPresent) {
            load();
        } else {
            save();
        }
    }

    private void lockFile() throws IOException {
        final FileLock fileLock;
        try {
            fileLock = fileChannel.tryLock();
        } catch (OverlappingFileLockException e) {
            throw new IOException("File is already locked: " + filePath, e);
        }
        if (fileLock == null) {
            throw new IOException("Another process holds an overlapping lock for file: " + filePath);
        }
    }

    private void load() throws IOException {
        fileChannel.position(0);
        JsonObject savedData = loadJson();
        final int version = savedData.getInt(CONFIG_KEY_VERSION, 0);
        if (version == 1) {
            parseDataV1(savedData);
        } else {
            throw new IOException("Unsupported checkpoint data version " + version + " from file: " + filePath);
        }
    }

    private JsonObject loadJson() throws IOException {
        JsonReader jsonReader = Json.createReader(fileReader);
        try {
            return jsonReader.readObject();
        } catch (RuntimeException e) {
            throw new IOException("Failed to parse checkpoint data from file: " + filePath, e);
        }
    }

    private void parseDataV1(JsonObject json) throws IOException {
        try {
            blockNumber = json.getJsonNumber(CONFIG_KEY_BLOCK).longValue();
            transactionIds = json.getJsonArray(CONFIG_KEY_TRANSACTIONS).getValuesAs(JsonString.class).stream()
                    .map(JsonString::getString)
                    .collect(Collectors.toCollection(HashSet::new));
        } catch (RuntimeException e) {
            throw new IOException("Bad format of checkpoint data from file: " + filePath, e);
        }
    }

    private void save() throws IOException {
        JsonObject jsonData = buildJson();
        fileChannel.position(0);
        saveJson(jsonData);
        fileChannel.truncate(fileChannel.position());
    }

    private JsonObject buildJson() {
        return Json.createObjectBuilder()
                    .add(CONFIG_KEY_VERSION, VERSION)
                    .add(CONFIG_KEY_BLOCK, blockNumber)
                    .add(CONFIG_KEY_TRANSACTIONS, Json.createArrayBuilder(transactionIds))
                    .build();
    }

    private void saveJson(JsonObject json) throws IOException {
        JsonWriter jsonWriter = Json.createWriter(fileWriter);
        try {
            jsonWriter.writeObject(json);
        } catch (RuntimeException e) {
            throw new IOException("Failed to write checkpoint data to file: " + filePath, e);
        }
        fileWriter.flush();
    }

    @Override
    public long getBlockNumber() {
        return blockNumber;
    }

    @Override
    public void setBlockNumber(long blockNumber) throws IOException {
        this.blockNumber = blockNumber;
        transactionIds.clear();
        save();
    }

    @Override
    public Set<String> getTransactionIds() {
        return Collections.unmodifiableSet(transactionIds);
    }

    @Override
    public void addTransactionId(String transactionId) throws IOException {
        transactionIds.add(transactionId);
        save();
    }

    @Override
    public void delete() throws IOException {
        close();
        Files.delete(filePath);
    }

    @Override
    public void close() throws IOException {
        fileChannel.close(); // Also releases lock
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(file=" + filePath +
                ", blockNumber=" + blockNumber +
                ", transactionIds=" + transactionIds + ")";
    }
}
