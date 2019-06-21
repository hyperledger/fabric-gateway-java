/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl.event;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.gateway.ContractEvent;
import org.hyperledger.fabric.gateway.spi.Checkpointer;
import org.hyperledger.fabric.gateway.spi.CommitListener;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.BlockInfo;
import org.hyperledger.fabric.sdk.Peer;

public final class Listeners {
    private static final Log LOG = LogFactory.getLog(Listeners.class);

    public static Consumer<BlockEvent> fromTransaction(Consumer<BlockEvent.TransactionEvent> listener) {
        return blockEvent -> blockEvent.getTransactionEvents().forEach(listener);
    }

    public static Consumer<BlockEvent> fromContract(Consumer<ContractEvent> listener) {
        return fromTransaction(transactionFromContract(listener));
    }

    private static Consumer<BlockEvent.TransactionEvent> transactionFromContract(Consumer<ContractEvent> listener) {
        return transactionEvent -> StreamSupport.stream(transactionEvent.getTransactionActionInfos().spliterator(), false)
                .map(BlockInfo.TransactionEnvelopeInfo.TransactionActionInfo::getEvent)
                .filter(Objects::nonNull)
                .map(chaincodeEvent -> new ContractEventImpl(transactionEvent, chaincodeEvent))
                .forEach(listener);
    }

    public static Consumer<BlockEvent> checkpointBlock(Checkpointer checkpointer, Consumer<BlockEvent> listener) {
        return blockEvent -> {
            final long eventBlockNumber = blockEvent.getBlockNumber();
            try {
                synchronized (checkpointer) {
                    long checkpointBlockNumber = checkpointer.getBlockNumber();

                    if (Checkpointer.UNSET_BLOCK_NUMBER == checkpointBlockNumber) {
                        // Record a starting block in case we don't complete handling and checkpoint below
                        checkpointBlockNumber = eventBlockNumber;
                        checkpointer.setBlockNumber(checkpointBlockNumber);
                    }

                    if (eventBlockNumber == checkpointBlockNumber) {
                        listener.accept(blockEvent); // Process event before checkpointing
                        checkpointer.setBlockNumber(eventBlockNumber + 1);
                    } else {
                        LOG.debug("Reject block number " + eventBlockNumber + " for checkpointer " + checkpointer);
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }

    public static Consumer<BlockEvent> checkpointTransaction(Checkpointer checkpointer, Consumer<BlockEvent.TransactionEvent> listener) {
        Consumer<BlockEvent.TransactionEvent> transactionListener = transactionEvent -> {
            String transactionId = transactionEvent.getTransactionID();
            try {
                synchronized (checkpointer) {
                    if (!checkpointer.getTransactionIds().contains(transactionId)) {
                        listener.accept(transactionEvent); // Process event before checkpointing
                        checkpointer.addTransactionId(transactionId);
                    } else {
                        LOG.debug("Reject transaction ID " + transactionId + " for checkpointer " + checkpointer);
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
        return checkpointBlock(checkpointer, fromTransaction(transactionListener));
    }

    public static Consumer<BlockEvent> checkpointContract(Checkpointer checkpointer, Consumer<ContractEvent> listener) {
        return checkpointTransaction(checkpointer, transactionFromContract(listener));
    }

    public static Consumer<ContractEvent> contract(Consumer<ContractEvent> listener, String chaincodeId) {
        return contractEvent -> {
            if (contractEvent.getChaincodeId().equals(chaincodeId)) {
                listener.accept(contractEvent);
            }
        };
    }

    public static Consumer<ContractEvent> contract(Consumer<ContractEvent> listener, String chaincodeId, Pattern namePattern) {
        return contract(contractEvent -> {
            if (namePattern.matcher(contractEvent.getName()).matches()) {
                listener.accept(contractEvent);
            }
        }, chaincodeId);
    }

    public static Consumer<BlockEvent.TransactionEvent> transaction(CommitListener listener, Collection<Peer> peers, String transactionId) {
        Set<Peer> peerSet = new HashSet<>(peers);
        return transactionEvent -> {
            if (transactionEvent.getTransactionID().equals(transactionId) && peerSet.contains(transactionEvent.getPeer())) {
                listener.acceptCommit(transactionEvent);
            }
        };
    }

    private Listeners() { }
}
