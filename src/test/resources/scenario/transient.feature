#
# Copyright 2019 IBM All Rights Reserved.
#
# SPDX-License-Identifier: Apache-2.0
#
Feature: Configure Fabric using SDK and submit/evaluate using a network Gateway
	Background:
		Given I have deployed a tls Fabric network
		And I have created and joined all channels from the tls common connection profile
		And I have created a gateway named test_gateway as user User1 within Org1 using the tls common connection profile

 	Scenario: Using a Gateway I can send transient data to instantiated node chaincode
		Given I install/instantiate node chaincode named fabcar at version 1.0.0 as fabcar01 to the tls Fabric network for all organizations on channel mychannel with endorsement policy 1AdminOr2Other and args [initLedger]
		When I use the gateway named test_transient to create a echoTransient transaction as txn01 for contract fabcar instantiated on channel mychannel
		And I set transient data on transaction txn01 to
			| key1 | value1 |
			| key2 | value2 |
		And I evaluate the transaction txn01 with args []
		Then The gateway named test_transient has a evaluate type JSON response matching
		    """
		    {
		    	"key1": "value1",
		    	"key2": "value2"
		    }
		    """
