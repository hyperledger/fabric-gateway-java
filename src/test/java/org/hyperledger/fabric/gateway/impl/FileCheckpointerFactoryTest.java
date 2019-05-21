/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import org.assertj.core.api.Assertions;
import org.hyperledger.fabric.gateway.TestUtils;
import org.hyperledger.fabric.gateway.spi.Checkpointer;
import org.hyperledger.fabric.gateway.spi.CheckpointerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

public class FileCheckpointerFactoryTest {
    private static final TestUtils testUtils = TestUtils.getInstance();

    private CheckpointerFactory factory;

    @BeforeEach
    public void beforeEach() throws IOException {
        Path directory = testUtils.createTempDirectory();
        factory = new FileCheckpointerFactory(directory);
    }

    private void assertNewCheckpointer(Checkpointer checkpointer) throws IOException {
        assertThat(checkpointer.getBlockNumber()).isEqualTo(Checkpointer.UNSET_BLOCK_NUMBER);
        assertThat(checkpointer.getTransactionIds()).isEmpty();
    }

    @Test
    public void create_throws_if_network_name_is_null() {
        assertThatThrownBy(() -> factory.create(null, "checkpointer"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("network");
    }

    @Test
    public void create_throws_if_network_name_is_empty() {
        assertThatThrownBy(() -> factory.create("", "checkpointer"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("network");
    }

    @Test
    public void create_throws_if_checkpointer_name_is_null() {
        Assertions.assertThatThrownBy(() -> factory.create("network", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("checkpointer");
    }

    @Test
    public void create_throws_if_checkpointer_name_is_empty() {
        Assertions.assertThatThrownBy(() -> factory.create("network", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("checkpointer");
    }

    @Test
    public void create_new_checkpointer_in_existing_directory() throws IOException {
        try (Checkpointer checkpointer = factory.create("network", "checkpointer")) {
            assertNewCheckpointer(checkpointer);
        }
    }

    @Test
    public void create_new_checkpointer_in_missing_directory() throws IOException {
        Path directory = testUtils.getUnusedDirectoryPath();
        factory = new FileCheckpointerFactory(directory);

        try (Checkpointer checkpointer = factory.create("network", "checkpointer")) {
            assertNewCheckpointer(checkpointer);
        }
    }

    @Test
    public void create_existing_checkpointer() throws IOException {
        String networkName = "network";
        String checkpointerName = "checkpointer";
        long blockNumber = 1L;
        String transactionId = "tx1";

        try (Checkpointer checkpointer = factory.create(networkName, checkpointerName)) {
            checkpointer.setBlockNumber(blockNumber);
            checkpointer.addTransactionId(transactionId);
        }

        try (Checkpointer checkpointer = factory.create(networkName, checkpointerName)) {
            assertThat(checkpointer.getBlockNumber()).isEqualTo(blockNumber);
            assertThat(checkpointer.getTransactionIds()).containsExactly(transactionId);
        }
    }
}
