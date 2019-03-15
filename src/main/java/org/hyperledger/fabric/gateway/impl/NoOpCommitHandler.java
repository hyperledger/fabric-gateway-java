package org.hyperledger.fabric.gateway.impl;

import org.hyperledger.fabric.gateway.spi.CommitHandler;

public enum NoOpCommitHandler implements CommitHandler {
    INSTANCE;

    @Override
    public void startListening() { }

    @Override
    public void waitForEvents() { }

    @Override
    public void cancelListening() { }
}
