/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.identity;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.hyperledger.fabric.gateway.TestUtils;
import org.hyperledger.fabric.gateway.spi.WalletStore;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FileSystemWalletStoreTest extends CommonWalletStoreTest {
    private static final TestUtils testUtils = TestUtils.getInstance();

    private Path storePath;

    @Override
    protected WalletStore newWalletStore() throws IOException {
        storePath = testUtils.createTempDirectory();
        return new FileSystemWalletStore(storePath);
    }

    @Test
    public void store_for_directory_that_does_not_exist_can_put_and_get_data() throws IOException {
        Path storePath = testUtils.getUnusedDirectoryPath().resolve("subdir");
        WalletStore store = new FileSystemWalletStore(storePath);

        store.put("label", asInputStream("data"));
        InputStream result = store.get("label");

        assertThat(asString(result)).isEqualTo("data");
    }

    @Test
    public void additional_files_in_store_directory_are_ignored() throws IOException {
        Path filePath = storePath.resolve("badfile");
        Files.createFile(filePath);

        Set<String> results = store.list();

        assertThat(results).isEmpty();
    }
}
