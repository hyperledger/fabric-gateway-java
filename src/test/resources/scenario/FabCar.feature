#
# Copyright 2019 IBM All Rights Reserved.
#
# SPDX-License-Identifier: Apache-2.0
#
Feature: Configure Fabric using SDK and submit/evaluate using a network Gateway
	Background:
		Given I have deployed a tls Fabric network
		And I have created and joined all channels from the tls connection profile
		And I deploy node chaincode named fabcar at version 1.0.0 for all organizations on channel mychannel with endorsement policy 1AdminOr2Other and arguments ["initLedger"]

 	Scenario: Using a Gateway I can submit and evaluate transactions on instantiated node chaincode
		Given I have a gateway as user User1 using the tls connection profile
		And I connect the gateway
		And I use the mychannel network
		When I prepare a createCar transaction for contract fabcar
	 	And I submit the transaction with arguments ["CAR10", "Trabant", "601 Estate", "brown", "Simon"]
		And I prepare a queryCar transaction for contract fabcar
	 	And I evaluate the transaction with arguments ["CAR10"]
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
