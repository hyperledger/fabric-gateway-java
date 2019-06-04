/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.gateway.DefaultCommitHandlers;
import org.hyperledger.fabric.gateway.DefaultQueryHandlers;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.GatewayException;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallet.Identity;
import org.hyperledger.fabric.gateway.spi.CheckpointerFactory;
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
import org.hyperledger.fabric.sdk.identity.X509Enrollment;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric.sdk.security.CryptoSuiteFactory;

import javax.json.Json;
import javax.json.JsonReader;
import javax.json.stream.JsonParsingException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public final class GatewayImpl implements Gateway {
    private static final Log LOG = LogFactory.getLog(Gateway.class);

    private final HFClient client;
    private final NetworkConfig networkConfig;
    private final Identity identity;
    private final Map<String, Network> networks = new HashMap<>();
    private final CommitHandlerFactory commitHandlerFactory;
    private final TimePeriod commitTimeout;
    private final QueryHandlerFactory queryHandlerFactory;
    private final boolean discovery;
    private final CheckpointerFactory checkpointerFactory;

    public static final class Builder implements Gateway.Builder {
        private CommitHandlerFactory commitHandlerFactory = DefaultCommitHandlers.MSPID_SCOPE_ALLFORTX;
        private TimePeriod commitTimeout = new TimePeriod(5, TimeUnit.MINUTES);
        private QueryHandlerFactory queryHandlerFactory = DefaultQueryHandlers.MSPID_SCOPE_SINGLE;
        private NetworkConfig ccp = null;
        private Identity identity = null;
        private HFClient client;
        private boolean discovery = false;
        private CheckpointerFactory checkpointerFactory = new FileCheckpointerFactory(Paths.get(System.getProperty("user.home"), ".hlf-checkpoint"));

        public Builder() {
        }

        @Override
		public Builder networkConfig(Path config) throws GatewayException {
			try {
				// ccp is either JSON or YAML
			    try (JsonReader reader = Json.createReader(new FileReader(config.toFile()))) {
			        reader.readObject();
					// looks like JSON
					ccp = NetworkConfig.fromJsonFile(config.toFile());
				} catch (JsonParsingException ex) {
					// assume YAML then
					ccp = NetworkConfig.fromYamlFile(config.toFile());
				}
			} catch (IOException | InvalidArgumentException | NetworkConfigurationException e) {
				throw new GatewayException(e);
			}
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
        public Builder queryHandler(QueryHandlerFactory queryHandler) {
            this.queryHandlerFactory = queryHandler;
            return this;
        }

        @Override
        public Builder commitTimeout(long timeout, TimeUnit timeUnit) {
            this.commitTimeout = new TimePeriod(timeout, timeUnit);
            return this;
        }

		@Override
		public Builder discovery(boolean enabled) {
			this.discovery = enabled;
			return this;
		}

		@Override
        public Builder checkpointer(CheckpointerFactory factory) {
            this.checkpointerFactory = factory;
            return this;
        }

        public Builder client(HFClient client) {
            this.client = client;
            return this;
        }

        @Override
        public GatewayImpl connect() throws GatewayException {
            return new GatewayImpl(this);
        }
    }

    private GatewayImpl(Builder builder) throws GatewayException {
        this.commitHandlerFactory = builder.commitHandlerFactory;
        this.commitTimeout = builder.commitTimeout;
        this.queryHandlerFactory = builder.queryHandlerFactory;
        this.discovery = builder.discovery;
        this.checkpointerFactory = builder.checkpointerFactory;

        if (builder.client != null) {
            // Only for testing!
            this.client = builder.client;
            this.networkConfig = null;

            User user = client.getUserContext();
            Enrollment enrollment = user.getEnrollment();
            this.identity = Identity.createIdentity(user.getMspId(), enrollment.getCert(), enrollment.getKey());
        } else {
            if (null == builder.identity) {
                throw new GatewayException("The gateway identity must be set");
            }
            if (null == builder.ccp) {
                throw new GatewayException("The network configuration must be specified");
            }
            this.networkConfig = builder.ccp;
            this.identity = builder.identity;

            this.client = createClient();
        }
    }

    private GatewayImpl(GatewayImpl that) throws GatewayException {
        this.commitHandlerFactory = that.commitHandlerFactory;
        this.commitTimeout = that.commitTimeout;
        this.queryHandlerFactory = that.queryHandlerFactory;
        this.discovery = that.discovery;
        this.checkpointerFactory = that.checkpointerFactory;
        this.networkConfig = that.networkConfig;
        this.identity = that.identity;

        this.client = createClient();
    }

    private HFClient createClient() throws GatewayException {
        Enrollment enrollment = new X509Enrollment(identity.getPrivateKey(), identity.getCertificate());
        User user = new User() {
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

        HFClient client = HFClient.createNewInstance();

        try {
            CryptoSuite cryptoSuite = CryptoSuiteFactory.getDefault().getCryptoSuite();
            client.setCryptoSuite(cryptoSuite);
            client.setUserContext(user);
        } catch (ClassNotFoundException | CryptoException | IllegalAccessException | NoSuchMethodException |
                InstantiationException | InvalidArgumentException | InvocationTargetException  e) {
            throw new GatewayException("Failed to configure client", e);
        }

        return client;
    }

    @Override
    public void close() {
    }

    @Override
    public synchronized Network getNetwork(final String networkName) throws GatewayException {
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
                    LOG.info("Unable to load channel configuration from connection profile: ", ex);
                }
            }
            if (channel == null) {
                try {
                    channel = client.newChannel(networkName);
                } catch (InvalidArgumentException e) {
                    // we've already checked the channel status
                	throw new GatewayException(e);
                }
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

    public boolean isDiscoveryEnabled() {
        return discovery;
    }

    public GatewayImpl newInstance() throws GatewayException {
        return new GatewayImpl(this);
    }

    public CheckpointerFactory getCheckpointerFactory() {
        return checkpointerFactory;
    }
}