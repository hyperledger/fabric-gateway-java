/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import org.hyperledger.fabric.gateway.DefaultCommitHandlers;
import org.hyperledger.fabric.gateway.DefaultQueryHandlers;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.GatewayException;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallet.Identity;
import org.hyperledger.fabric.gateway.spi.CommitHandlerFactory;
import org.hyperledger.fabric.gateway.spi.QueryHandlerFactory;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.NetworkConfig;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.NetworkConfigurationException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.hyperledger.fabric.sdk.identity.X509Enrollment;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric.sdk.security.CryptoSuiteFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class GatewayImpl implements Gateway {
    private final HFClient client;
    private final NetworkConfig networkConfig;
    private final Identity identity;
    private final Map<String, Network> networks = new HashMap<>();
    private final CommitHandlerFactory commitHandlerFactory;
    private final TimePeriod commitTimeout;
    private final QueryHandlerFactory queryHandlerFactory;

    public static class Builder implements Gateway.Builder {
        private CommitHandlerFactory commitHandlerFactory = DefaultCommitHandlers.NETWORK_SCOPE_ALLFORTX;
        private TimePeriod commitTimeout = new TimePeriod(5, TimeUnit.MINUTES);
        private QueryHandlerFactory queryHandlerFactory = DefaultQueryHandlers.MSPID_SCOPE_SINGLE;
        private Path ccp = null;
        private Identity identity = null;
        private User user = null;
        private HFClient client;

        public Builder() {
        }

        @Override
        public Builder networkConfig(Path config) {
            this.ccp = config;
            return this;
        }

        @Override
        public Builder identity(Wallet wallet, String id) throws GatewayException {
            this.identity = wallet.get(id);
            return this;
        }

        @Override
        public Builder commitHandler(CommitHandlerFactory commitHandlerFactory) {
            this.commitHandlerFactory = commitHandlerFactory;
            return this;
        }

        @Override
        public Gateway.Builder queryHandler(QueryHandlerFactory queryHandler) {
            this.queryHandlerFactory = queryHandler;
            return this;
        }

        @Override
        public Gateway.Builder commitTimeout(long timeout, TimeUnit timeUnit) {
            this.commitTimeout = new TimePeriod(timeout, timeUnit);
            return this;
        }

        public Builder client(HFClient client) {
            this.client = client;
            return this;
        }

        @Override
        public Gateway connect() throws GatewayException {
            return new GatewayImpl(this);
        }

        private User createUser() {
            if (user != null) {
                return user;
            } else if (identity != null) {
                Enrollment enrollment = new X509Enrollment(identity.getPrivateKey(), identity.getCertificate());
                return new User() {

                    @Override
                    public String getName() {
                        return "gateway";
                    }

                    @Override
                    public Set<String> getRoles() {
                        return Collections.emptySet();
                    }

                    @Override
                    public String getAccount() {
                        return "";
                    }

                    @Override
                    public String getAffiliation() {
                        return "";
                    }

                    @Override
                    public Enrollment getEnrollment() {
                        return enrollment;
                    }

                    @Override
                    public String getMspId() {
                        return identity.getMspId();
                    }
                };
            }
            return null;
        }
    }

    private GatewayImpl(Builder builder) throws GatewayException {
        try {
            if (builder.client != null) {
                this.client = builder.client;
                this.identity = null;
                this.networkConfig = null;
            } else {
                if (builder.identity == null) {
                    throw new GatewayException("The gateway identity must be set");
                }
                if (builder.ccp == null) {
                    throw new GatewayException("The network configuration must be specified");
                }
                this.client = HFClient.createNewInstance();
                CryptoSuite cryptoSuite;
                cryptoSuite = CryptoSuiteFactory.getDefault().getCryptoSuite();
                this.client.setCryptoSuite(cryptoSuite);
                this.networkConfig = NetworkConfig.fromJsonFile(builder.ccp.toFile());
                this.identity = builder.identity;
                this.client.setUserContext(builder.createUser());
            }
            this.commitHandlerFactory = builder.commitHandlerFactory;
            this.commitTimeout = builder.commitTimeout;
            this.queryHandlerFactory = builder.queryHandlerFactory;
        } catch (InvalidArgumentException | NetworkConfigurationException | IOException | CryptoException | ClassNotFoundException | IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            throw new GatewayException(e);
        }
    }

    @Override
    public void close() {
    }

    @Override
    public synchronized Network getNetwork(String networkName) throws GatewayException {
        if (networkName == null || networkName.isEmpty()) {
            throw new IllegalArgumentException("Channel name must be a non-empty string");
        }
        Network network = networks.get(networkName);
        if (network == null) {
            Channel channel = client.getChannel(networkName);
            if (channel == null && networkConfig != null) {
                try {
                    channel = client.loadChannelFromConfig(networkName, networkConfig);
                } catch (InvalidArgumentException | NetworkConfigurationException ex) {
                    // ignore
                }
            }
            if (channel == null) {
                try {
                    channel = client.newChannel(networkName);
                } catch (InvalidArgumentException e) {
                    // we've already checked the channel status
                }
            }
            try {
                channel.initialize();
            } catch (InvalidArgumentException | TransactionException e) {
                throw new GatewayException(e);
            }
            network = new NetworkImpl(channel, this);
            networks.put(networkName, network);
        }
        return network;
    }

    @Override
    public Wallet.Identity getIdentity() {
        return identity;
    }

    public HFClient getClient() {
        return client;
    }

    public CommitHandlerFactory getCommitHandlerFactory() {
        return commitHandlerFactory;
    }

    public TimePeriod getCommitTimeout() {
        return commitTimeout;
    }

    public QueryHandlerFactory getQueryHandlerFactory() {
        return queryHandlerFactory;
    }
}