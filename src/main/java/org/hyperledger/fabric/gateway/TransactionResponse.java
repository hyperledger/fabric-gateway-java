package org.hyperledger.fabric.gateway;

import java.io.Serializable;

public class TransactionResponse implements Serializable {

    private final String transactionId;
    private final byte[] payload;

    public TransactionResponse(String transactionId, byte[] payload) {
        this.transactionId = transactionId;
        this.payload = payload;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public byte[] getPayload() {
        return payload;
    }
}
