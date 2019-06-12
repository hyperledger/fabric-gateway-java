/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.spi;

import org.hyperledger.fabric.sdk.BlockEvent;

/**
 * Implemented by listeners for transaction commit events. The listener is notified both of commit events received
 * from peers and also of peer communication failures. A commit event may still be received from a peer following a
 * disconnect event.
 */
public interface CommitListener {
    /**
     * Called to notify the listener that a given peer has processed a transaction.
     * @param transactionEvent Transaction event information.
     */
    void acceptCommit(BlockEvent.TransactionEvent transactionEvent);

    /**
     * Called to notify the listener of a communication failure with a given peer.
     * @param disconnectEvent Disconnect event information.
     */
    void acceptDisconnect(PeerDisconnectEvent disconnectEvent);
}
