/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Utility functions.
 */
public final class GatewayUtils {
    // Private constructor to prevent instantiation
    private GatewayUtils() { }

    public static String toString(final Object o) {
        return o != null ? o.getClass().getSimpleName() + '@' + Integer.toHexString(System.identityHashCode(o)) : "null";
    }

    public static String toString(final Object o, final String... additionalInfo) {
        return toString(o) + Arrays.stream(additionalInfo)
                .collect(Collectors.joining(", ", "(", ")"));
    }

    public static void copy(final InputStream input, final OutputStream output) throws IOException {
        for (int b; (b = input.read()) >= 0; ) { // checkstyle:ignore-line:InnerAssignment
            output.write(b);
        }
    }
}
