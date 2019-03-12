/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import java.util.concurrent.TimeoutException;

import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.GatewayException;
import org.hyperledger.fabric.gateway.Transaction;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;

public class ContractImpl implements Contract {
  private final NetworkImpl network;
  private final GatewayImpl gateway;
  private final String chaincodeId;
  private final String name;

  ContractImpl(NetworkImpl network, String chaincodeId, String name) {
    this.network = network;
    this.gateway = network.getGateway();
    this.chaincodeId = chaincodeId;
    this.name = name;
  }

  @Override
  public Transaction createTransaction(String name) {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("Transaction must be a non-empty string");
    }
    String qualifiedName = getQualifiedName(name);
    TransactionProposalRequest proposalRequest = gateway.getClient().newTransactionProposalRequest();
    proposalRequest.setChaincodeID(ChaincodeID.newBuilder().setName(chaincodeId).build());
    proposalRequest.setFcn(qualifiedName);
    Transaction txn = new TransactionImpl(proposalRequest, network.getChannel());
    return txn;
  }

  @Override
  public byte[] submitTransaction(String name, String... args) throws GatewayException, TimeoutException {
    return createTransaction(name).submit(args);
  }

  @Override
  public byte[] evaluateTransaction(String name, String... args) throws GatewayException {
    return createTransaction(name).evaluate(args);
  }

  private String getQualifiedName(String tname) {
    return this.name.isEmpty() ? tname : this.name + ':' + tname;
  }
}
