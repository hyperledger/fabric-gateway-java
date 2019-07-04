/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.Set;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonWriter;

import org.apache.commons.io.FileUtils;
import org.hyperledger.fabric.gateway.Wallet;

public final class FileSystemWallet implements Wallet {
    private Path basePath;

    public FileSystemWallet(Path path) throws IOException {
        boolean walletExists = Files.isDirectory(path);
        if (!walletExists) {
            Files.createDirectories(path);
        }
        basePath = path;
    }

    @Override
    public void put(String label, Identity identity) throws IOException {
        Path idFolder = basePath.resolve(label);
        if (!Files.isDirectory(idFolder)) {
            Files.createDirectories(idFolder);
        }
        Path idFile = basePath.resolve(Paths.get(label, label));
        try (Writer fw = Files.newBufferedWriter(idFile)) {
            String json = toJson(label, identity);
            fw.append(json);
        }

        Path pemFile = basePath.resolve(Paths.get(label, label + "-priv"));
        writePrivateKey(identity.getPrivateKey(), pemFile);
    }

    @Override
    public Identity get(String label) throws IOException {
        Path idFile = basePath.resolve(Paths.get(label, label));
        if (Files.exists(idFile)) {
            try (BufferedReader fr = Files.newBufferedReader(idFile)) {
                String contents = fr.readLine();
                return fromJson(contents);
            }
        }
        return null;
    }

    @Override
    public Set<String> getAllLabels() {
        return Arrays.stream(basePath.toFile().listFiles(File::isDirectory))
                .map(File::getName)
                .collect(Collectors.toSet());
    }

    @Override
    public void remove(String label) throws IOException {
        Path idDir = basePath.resolve(label);
        if (Files.exists(idDir)) {
            FileUtils.deleteDirectory(idDir.toFile());
        }
    }

    @Override
    public boolean exists(String label) {
        Path idFile = basePath.resolve(Paths.get(label, label));
        return Files.exists(idFile);
    }

    private Identity fromJson(String json) throws IOException {
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            JsonObject idObject = reader.readObject();
            String name = idObject.getString("name");  // TODO assert this is the same as the folder
            String mspId = idObject.getString("mspid");
            JsonObject enrollment = idObject.getJsonObject("enrollment");
            String signingId = enrollment.getString("signingIdentity");
            Path pemFile = basePath.resolve(Paths.get(name, signingId + "-priv"));
            String certificate = enrollment.getJsonObject("identity").getString("certificate");
            return Identity.createIdentity(mspId, new StringReader(certificate), Files.newBufferedReader(pemFile));
        }
    }

    private static String toJson(String name, Identity identity) {
        String json = null;
        JsonObject idObject = Json.createObjectBuilder()
                .add("name", name)
                .add("type", "X509")
                .add("mspid", identity.getMspId())
                .add("enrollment", Json.createObjectBuilder()
                        .add("signingIdentity", name)
                        .add("identity", Json.createObjectBuilder()
                                .add("certificate", identity.getCertificate())))
                .build();

        StringWriter writer = new StringWriter();
        try (JsonWriter jw = Json.createWriter(writer)) {
            jw.writeObject(idObject);
        }
        json = writer.toString();
        return json;
    }

    private static void writePrivateKey(PrivateKey key, Path pemFile) throws IOException  {
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(pemFile))) {
            writer.println("-----BEGIN PRIVATE KEY-----");
            String base64 = Base64.getEncoder().encodeToString(key.getEncoded());
            Arrays.stream(base64.split("(?<=\\G.{64})")).forEachOrdered(line -> writer.println(line));
            writer.println("-----END PRIVATE KEY-----");
        }
    }
}
