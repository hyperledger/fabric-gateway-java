package org.hyperledger.fabric.gateway.impl;

import org.hyperledger.fabric.gateway.GatewayException;
import org.hyperledger.fabric.gateway.spi.CommitHandler;

import java.util.concurrent.TimeoutException;

public class NoOpCommitHandler implements CommitHandler {
    public static final CommitHandler INSTANCE = new NoOpCommitHandler();

    /**
     * Private constructor to prevent instantiation, other than as singleton instance.
     */
    private NoOpCommitHandler() { }

    @Override
    public void startListening() { }

    @Override
    public void waitForEvents() throws GatewayException, TimeoutException { }

    @Override
    public void cancelListening() { }
}
