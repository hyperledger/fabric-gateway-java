/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.gateway.*;
import org.hyperledger.fabric.gateway.impl.identity.X509IdentityProvider;
import org.hyperledger.fabric.gateway.spi.CommitHandlerFactory;
import org.hyperledger.fabric.gateway.spi.QueryHandlerFactory;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.Channel.PeerOptions;
import org.hyperledger.fabric.sdk.Peer.PeerRole;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.NetworkConfigurationException;

import java.io.*;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public final class GatewayImpl implements Gateway {
    private static final Log LOG = LogFactory.getLog(Gateway.class);

    private static final long DEFAULT_COMMIT_TIMEOUT = 5;
    private static final TimeUnit DEFAULT_COMMIT_TIMEOUT_UNIT = TimeUnit.MINUTES;

    private final HFClient client;
    private final NetworkConfig networkConfig;
    private final Identity identity;
    private final Map<String, NetworkImpl> networks = new HashMap<>();
    private final CommitHandlerFactory commitHandlerFactory;
    private final TimePeriod commitTimeout;
    private final QueryHandlerFactory queryHandlerFactory;
    private final boolean discovery;
    private final boolean deliverFilter;

    public static final class Builder implements Gateway.Builder {
        private CommitHandlerFactory commitHandlerFactory = DefaultCommitHandlers.PREFER_MSPID_SCOPE_ALLFORTX;
        private TimePeriod commitTimeout = new TimePeriod(DEFAULT_COMMIT_TIMEOUT, DEFAULT_COMMIT_TIMEOUT_UNIT);
        private QueryHandlerFactory queryHandlerFactory = DefaultQueryHandlers.PREFER_MSPID_SCOPE_SINGLE;
        private NetworkConfig ccp = null;
        private Identity identity = null;
        private HFClient client;
        private boolean discovery = false;
        private boolean deliverFilter = false;

        private static final class ExposedByteArrayOutputStream extends ByteArrayOutputStream {
            public byte[] getInternalBuffer() {
                return buf;
            }
        }

        @Override
        public Builder networkConfig(final Path config) throws IOException {
            try (InputStream fileIn = new FileInputStream(config.toFile());
                 InputStream bufferedIn = new BufferedInputStream(fileIn)) {
                return networkConfig(bufferedIn);
            }
        }

        @Override
        public Builder networkConfig(final InputStream config) throws IOException {
            try (InputStream bufferedStream = copyToMemory(config)) {
                try {
                    ccp = NetworkConfig.fromJsonStream(bufferedStream);
                } catch (Exception e) {
                    bufferedStream.reset();
                    ccp = NetworkConfig.fromYamlStream(bufferedStream);
                }
            } catch (NetworkConfigurationException e) {
                throw new IOException(e);
            }
            return this;
        }

        private InputStream copyToMemory(final InputStream in) throws IOException {
            ExposedByteArrayOutputStream outBuff = new ExposedByteArrayOutputStream();
            GatewayUtils.copy(in, outBuff);
            return new ByteArrayInputStream(outBuff.getInternalBuffer(), 0, outBuff.size());
        }

        @Override
        public Builder identity(final Wallet wallet, final String id) throws IOException {
            this.identity = wallet.get(id);
            if (null == identity) {
                throw new IllegalArgumentException("Identity not found in wallet: " + id);
            }
            return this;
        }

        @Override
        public Builder identity(final Identity identity) {
            if (null == identity) {
                throw new IllegalArgumentException("Identity must not be null");
            }
            if (!(identity instanceof X509Identity)) {
                throw new IllegalArgumentException("No provider for identity type: " + identity.getClass().getName());
            }
            this.identity = identity;
            return this;
        }

        @Override
        public Builder commitHandler(final CommitHandlerFactory commitHandlerFactory) {
            this.commitHandlerFactory = commitHandlerFactory;
            return this;
        }

        @Override
        public Builder queryHandler(final QueryHandlerFactory queryHandler) {
            this.queryHandlerFactory = queryHandler;
            return this;
        }

        @Override
        public Builder commitTimeout(final long timeout, final TimeUnit timeUnit) {
            this.commitTimeout = new TimePeriod(timeout, timeUnit);
            return this;
        }

        @Override
        public Builder discovery(final boolean enabled) {
            this.discovery = enabled;
            return this;
        }

        @Override
        public Builder deliverFilter(final boolean enabled) {
            this.deliverFilter = enabled;
            return this;
        }

        public Builder client(final HFClient client) {
            this.client = client;
            return this;
        }

        @Override
        public GatewayImpl connect() {
            return new GatewayImpl(this);
        }
    }

    private GatewayImpl(final Builder builder) {
        this.commitHandlerFactory = builder.commitHandlerFactory;
        this.commitTimeout = builder.commitTimeout;
        this.queryHandlerFactory = builder.queryHandlerFactory;
        this.discovery = builder.discovery;
        this.deliverFilter = builder.deliverFilter;

        if (builder.client != null) {
            // Only for testing!
            this.client = builder.client;
            this.networkConfig = null;

            User user = client.getUserContext();
            Enrollment enrollment = user.getEnrollment();
            try {
                this.identity = Identities.newX509Identity(user.getMspId(), user.getEnrollment());
            } catch (CertificateException e) {
                throw new GatewayRuntimeException(e);
            }
        } else {
            if (null == builder.identity) {
                throw new IllegalStateException("The gateway identity must be set");
            }
            if (null == builder.ccp) {
                throw new IllegalStateException("The network configuration must be specified");
            }
            this.networkConfig = builder.ccp;
            this.identity = builder.identity;

            this.client = createClient();
        }
    }

    private GatewayImpl(final GatewayImpl that) {
        this.commitHandlerFactory = that.commitHandlerFactory;
        this.commitTimeout = that.commitTimeout;
        this.queryHandlerFactory = that.queryHandlerFactory;
        this.discovery = that.discovery;
        this.deliverFilter = that.deliverFilter;
        this.networkConfig = that.networkConfig;
        this.identity = that.identity;

        this.client = createClient();
    }

    private HFClient createClient() {
        HFClient client = HFClient.createNewInstance();
        // Hard-coded type for now but needs to get appropriate provider from wallet (or registry)
        X509IdentityProvider.INSTANCE.setUserContext(client, identity, "gateway");
        return client;
    }

    @Override
    public synchronized void close() {
        networks.values().forEach(NetworkImpl::close);
        networks.clear();
    }

    @Override
    public synchronized Network getNetwork(final String networkName) {
        if (networkName == null || networkName.isEmpty()) {
            throw new IllegalArgumentException("Channel name must be a non-empty string");
        }

        NetworkImpl network = networks.get(networkName);
        if (network == null) {
            Channel channel = client.getChannel(networkName);
            if (channel == null && networkConfig != null) {
                try {
                    channel =
//                            client.loadChannelFromConfig(
//                                    networkName,
//                                    networkConfig,
//                                    (networkConfig, client, channelArg, peerName, peerURL, peerProperties, peerOptions, jsonPeer) -> {
//                                        try {
//                                            Peer peer = client.newPeer(peerName, peerURL, peerProperties);
//                                            if (this.isDeliverFilter()) {
//                                                peerOptions.registerEventsForFilteredBlocks();
//                                            }
//                                            channelArg.addPeer(peer, peerOptions);
//                                        } catch (Exception e) {
//                                            throw new NetworkConfigurationException(
//                                                    format(
//                                                            "Error on creating channel %s peer %s",
//                                                            channelArg.getName(),
//                                                            peerName
//                                                    ), e);
//                                        }
//                                    },
//                                    (networkConfig, client, channelArg, ordererName, ordererURL, ordererProperties, jsonOrderer) -> {
//                                        try {
//                                            Orderer orderer = client.newOrderer(ordererName, ordererURL, ordererProperties);
//                                            channelArg.addOrderer(orderer);
//                                        } catch (Exception e) {
//                                            throw new NetworkConfigurationException(
//                                                    format(
//                                                            "Error on creating channel %s orderer %s",
//                                                            channelArg.getName(),
//                                                            ordererName
//                                                    ), e);
//                                        }
//                                    }
//                            );
                            client.loadChannelFromConfig(
                                    networkName,
                                    networkConfig
                            );
                } catch (InvalidArgumentException | NetworkConfigurationException ex) {
                    LOG.info("Unable to load channel configuration from connection profile: " + ex.getLocalizedMessage());
                }
            }
            if (channel == null) {
                try {
                    // since this channel is not in the CCP, we'll assume it exists,
                    // and the org's peer(s) has joined it with all roles
                    channel = client.newChannel(networkName);
                    for (Peer peer : getPeersForOrg()) {

                        PeerOptions peerOptions = PeerOptions.createPeerOptions();
                        if (this.isDeliverFilter()) {
                            peerOptions.registerEventsForFilteredBlocks();
                        }

                        peerOptions.setPeerRoles(EnumSet.allOf(PeerRole.class));
                        channel.addPeer(peer, peerOptions);
                    }
                } catch (InvalidArgumentException e) {
                    // we've already checked the channel status
                    throw new GatewayRuntimeException(e);
                }
            }
            network = new NetworkImpl(channel, this);
            networks.put(networkName, network);
        }
        return network;
    }

    @Override
    public Identity getIdentity() {
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

    public boolean isDeliverFilter() {
        return deliverFilter;
    }

    public GatewayImpl newInstance() {
        return new GatewayImpl(this);
    }

    private Collection<Peer> getPeersForOrg() {
        Collection<Peer> peers = new ArrayList<>();
        List<String> peerNames = networkConfig.getClientOrganization().getPeerNames();
        for (String name : peerNames) {
            try {
                String url = networkConfig.getPeerUrl(name);
                Properties props = networkConfig.getPeerProperties(name);
                peers.add(client.newPeer(name, url, props));
            } catch (InvalidArgumentException e) {
                // log warning
            }
        }
        return peers;
    }
}
