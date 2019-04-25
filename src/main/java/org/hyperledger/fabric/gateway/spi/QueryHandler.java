/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.spi;

import org.hyperledger.fabric.gateway.GatewayException;
import org.hyperledger.fabric.sdk.ProposalResponse;

public interface QueryHandler {
    ProposalResponse evaluate(Query query) throws GatewayException;
}
