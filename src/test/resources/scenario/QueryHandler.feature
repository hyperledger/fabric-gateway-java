#
# Copyright 2019 IBM All Rights Reserved.
#
# SPDX-License-Identifier: Apache-2.0
#
Feature: Query handler strategies for evaluating transactions
	Background:
		Given I have deployed a tls Fabric network
		And I have created and joined all channels from the tls connection profile
		And I deploy node chaincode named fabcar at version 1.0.0 for all organizations on channel mychannel with endorsement policy 1AdminOr2Other and arguments ["initLedger"]

 	Scenario: Evaluate transaction using MSPID_SCOPE_SINGLE query handler
		Given I have a gateway as user User1 using the tls connection profile
		And I configure the gateway to use the default MSPID_SCOPE_SINGLE query handler
		And I connect the gateway
		And I use the mychannel network
		When I prepare a createCar transaction for contract fabcar
	 	And I submit the transaction with arguments ["MSPID_SCOPE_SINGLE", "Trabant", "601 Estate", "brown", "Simon"]
		And I prepare a queryCar transaction for contract fabcar
	 	And I evaluate the transaction with arguments ["MSPID_SCOPE_SINGLE"]
		Then the response should be JSON matching
		    """
		    {
		    	"color": "brown",
		    	"docType": "car",
		    	"make": "Trabant",
		    	"model": "601 Estate",
		    	"owner": "Simon"
		    }
		    """

 	Scenario: Evaluate transaction using MSPID_SCOPE_ROUND_ROBIN query handler
		Given I have a gateway as user User1 using the tls connection profile
		And I configure the gateway to use the default MSPID_SCOPE_ROUND_ROBIN query handler
		And I connect the gateway
		And I use the mychannel network
		When I prepare a createCar transaction for contract fabcar
	 	And I submit the transaction with arguments ["MSPID_SCOPE_ROUND_ROBIN", "Trabant", "601 Estate", "brown", "Simon"]
		And I prepare a queryCar transaction for contract fabcar
	 	And I evaluate the transaction with arguments ["MSPID_SCOPE_ROUND_ROBIN"]
		Then the response should be JSON matching
		    """
		    {
		    	"color": "brown",
		    	"docType": "car",
		    	"make": "Trabant",
		    	"model": "601 Estate",
		    	"owner": "Simon"
		    }
		    """
