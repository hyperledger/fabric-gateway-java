/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import org.hyperledger.fabric.gateway.TestUtils;
import org.hyperledger.fabric.gateway.spi.Checkpointer;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

public class FileCheckpointerTest {
    private static final TestUtils testUtils = TestUtils.getInstance();

    private void writeToFile(Path file, String text) throws IOException {
        BufferedWriter writer = Files.newBufferedWriter(file);
        writer.write(text);
        writer.flush();
        writer.close();
    }

    @Test
    public void checkpointer_for_file_without_checkpoint_data_throws() throws IOException {
        Path file = testUtils.createTempFile();
        assertThatThrownBy(() -> new FileCheckpointer(file))
                .isInstanceOf(IOException.class);
    }

    @Test
    public void checkpointer_for_missing_file_is_has_unset_block_number() throws IOException {
        Path file = testUtils.getUnusedFilePath();
        try (Checkpointer checkpointer = new FileCheckpointer(file)) {
            long blockNumber = checkpointer.getBlockNumber();
            assertThat(blockNumber).isEqualTo(Checkpointer.UNSET_BLOCK_NUMBER);
        }
    }

    @Test
    public void checkpointer_for_missing_file_has_no_transactions() throws IOException {
        Path file = testUtils.getUnusedFilePath();
        try (Checkpointer checkpointer = new FileCheckpointer(file)) {
            Set<String> transactionIds = checkpointer.getTransactionIds();
            assertThat(transactionIds).isEmpty();
        }
    }

    @Test
    public void set_block_number() throws IOException {
        Path file = testUtils.getUnusedFilePath();
        long expectedBlockNumber = 1L;
        Checkpointer checkpointer = new FileCheckpointer(file);

        checkpointer.setBlockNumber(expectedBlockNumber);

        long actualBlockNumber = checkpointer.getBlockNumber();
        assertThat(actualBlockNumber).isEqualTo(expectedBlockNumber);
    }

    @Test
    public void add_transaction() throws IOException {
        Path file = testUtils.getUnusedFilePath();
        String transactionId = "tx1";
        Checkpointer checkpointer = new FileCheckpointer(file);

        checkpointer.addTransactionId(transactionId);

        Set<String> transactionIds = checkpointer.getTransactionIds();
        assertThat(transactionIds).containsExactly(transactionId);
    }

    @Test
    public void get_transactions_does_not_allow_modification_of_internal_state() throws IOException {
        Path file = testUtils.getUnusedFilePath();
        Checkpointer checkpointer = new FileCheckpointer(file);

        assertThatThrownBy(() -> checkpointer.getTransactionIds().add("tx1"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void set_block_number_clears_transactions() throws IOException {
        Path file = testUtils.getUnusedFilePath();
        Checkpointer checkpointer = new FileCheckpointer(file);

        checkpointer.addTransactionId("tx1");
        checkpointer.setBlockNumber(1L);

        Set<String> transactionIds = checkpointer.getTransactionIds();
        assertThat(transactionIds).isEmpty();
    }

    @Test
    public void checkpointer_locks_file() throws IOException {
        Path file = testUtils.getUnusedFilePath();
        new FileCheckpointer(file);

        assertThatThrownBy(() -> new FileCheckpointer(file))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("File is already locked")
                .hasMessageContaining(file.toString());
    }

    @Test
    public void close_unlocks_file() throws IOException {
        Path file = testUtils.getUnusedFilePath();
        Checkpointer checkpointer = new FileCheckpointer(file);
        checkpointer.close();

        assertThatCode(() -> new FileCheckpointer(file))
                .doesNotThrowAnyException();
    }

    @Test
    public void persists_block_number() throws IOException {
        Path file = testUtils.getUnusedFilePath();
        long expectedBlockNumber = 1L;

        try (Checkpointer checkpointer = new FileCheckpointer(file)) {
            checkpointer.setBlockNumber(expectedBlockNumber);
        }

        try (Checkpointer checkpointer = new FileCheckpointer(file)) {
            long actualBlockNumber = checkpointer.getBlockNumber();
            assertThat(actualBlockNumber).isEqualTo(expectedBlockNumber);
        }
    }

    @Test
    public void persists_transactions() throws IOException {
        Path file = testUtils.getUnusedFilePath();
        String transactionId = "tx1";

        try (Checkpointer checkpointer = new FileCheckpointer(file)) {
            checkpointer.addTransactionId(transactionId);
        }

        try (Checkpointer checkpointer = new FileCheckpointer(file)) {
            Set<String> transactionIds = checkpointer.getTransactionIds();
            assertThat(transactionIds).containsExactly(transactionId);
        }
    }

    @Test
    public void throws_on_malformed_json() throws IOException {
        Path file = testUtils.createTempFile();
        writeToFile(file, "{ \"version\": 1"); // Missing closing brace

        assertThatThrownBy(() -> new FileCheckpointer(file))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Failed to parse")
                .hasMessageContaining(file.toString());
    }

    @Test
    public void throws_on_invalid_checkpoint_data() throws IOException {
        Path file = testUtils.createTempFile();
        writeToFile(file, "{ \"version\": 1 }"); // Missing data

        assertThatThrownBy(() -> new FileCheckpointer(file))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Bad format of checkpoint data")
                .hasMessageContaining(file.toString());
    }

    @Test
    public void throws_on_unsupported_checkpoint_data_version() throws IOException {
        Path file = testUtils.createTempFile();
        writeToFile(file, "{ \"version\": 0 }");

        assertThatThrownBy(() -> new FileCheckpointer(file))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Unsupported checkpoint data version")
                .hasMessageContaining(file.toString());
    }

    @Test void delete_removes_data() throws IOException {
        Path file = testUtils.getUnusedFilePath();

        try (Checkpointer checkpointer = new FileCheckpointer(file)) {
            checkpointer.setBlockNumber(1L);
            checkpointer.delete();
        }

        try (Checkpointer checkpointer = new FileCheckpointer(file)) {
            long blockNumber = checkpointer.getBlockNumber();
            assertThat(blockNumber).isEqualTo(Checkpointer.UNSET_BLOCK_NUMBER);
        }
    }

}
