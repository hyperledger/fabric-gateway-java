/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package scenario;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;

import org.hyperledger.fabric.gateway.Transaction;
import org.hyperledger.fabric.sdk.Peer;

import static org.assertj.core.api.Assertions.assertThat;

public final class TransactionInvocation {
    private final Transaction transaction;
    private final boolean expectSuccess;
    private String response;
    private Throwable error;

    public static TransactionInvocation expectFail(Transaction transaction) {
        return new TransactionInvocation(transaction, false);
    }

    public static TransactionInvocation expectSuccess(Transaction transaction) {
        return new TransactionInvocation(transaction, true);
    }

    private TransactionInvocation(Transaction transaction, boolean expectSuccess) {
        this.transaction = transaction;
        this.expectSuccess = expectSuccess;
    }

    public void setTransient(Map<String, byte[]> transientData) {
        transaction.setTransient(transientData);
    }

    public void setEndorsingPeers(Collection<Peer> peers) {
        transaction.setEndorsingPeers(peers);
    }

    public void submit(String... args) {
        invoke(() -> transaction.submit(args));
    }

    public void evaluate(String... args) {
        invoke(() -> transaction.evaluate(args));
    }

    private void invoke(Callable<byte[]> invocationFn) {
        try {
            byte[] result = invocationFn.call();
            setResponse(result);
        } catch (Exception e) {
            setError(e);
        }
    }

    private void setResponse(byte[] response) {
        String text = ScenarioSteps.newString(response);
        assertThat(expectSuccess)
                .withFailMessage("Response received for transaction that was expected to fail: %s", text)
                .isTrue();
        this.response = text;
        this.error = null;
    }

    private void setError(Throwable error) {
        assertThat(expectSuccess)
                .withFailMessage("Error received for transaction that was expected to succeed: %s", error)
                .isFalse();
        this.response = null;
        this.error = error;
    }

    public String getResponse() {
        assertThat(response)
                .withFailMessage("No transaction response")
                .isNotNull();
        return response;
    }

    public Throwable getError() {
        assertThat(error)
                .withFailMessage("No transaction error")
                .isNotNull();
        return error;
    }
}
