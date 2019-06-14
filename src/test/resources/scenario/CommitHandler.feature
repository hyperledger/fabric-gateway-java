#
# Copyright 2019 IBM All Rights Reserved.
#
# SPDX-License-Identifier: Apache-2.0
#
Feature: Commit handler strategies for submitting transactions
	Background:
		Given I have deployed a tls Fabric network
		And I have created and joined all channels from the tls connection profile
		And I deploy node chaincode named fabcar at version 1.0.0 for all organizations on channel mychannel with endorsement policy 1AdminOr2Other and arguments ["initLedger"]

 	Scenario: Submit transaction using MSPID_SCOPE_ALLFORTX commit handler
		Given I have a gateway as user User1 using the tls connection profile
		And I configure the gateway to use the default MSPID_SCOPE_ALLFORTX commit handler
		And I connect the gateway
		And I use the mychannel network
		And I use the fabcar contract
		When I prepare a createCar transaction
	 	And I submit the transaction with arguments ["MSPID_SCOPE_ALLFORTX", "Trabant", "601 Estate", "brown", "Simon"]
		And I prepare a queryCar transaction
	 	And I evaluate the transaction with arguments ["MSPID_SCOPE_ALLFORTX"]
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

 	Scenario: Submit transaction using MSPID_SCOPE_ANYFORTX commit handler
		Given I have a gateway as user User1 using the tls connection profile
		And I configure the gateway to use the default MSPID_SCOPE_ANYFORTX commit handler
		And I connect the gateway
		And I use the mychannel network
		And I use the fabcar contract
		When I prepare a createCar transaction
	 	And I submit the transaction with arguments ["MSPID_SCOPE_ANYFORTX", "Trabant", "601 Estate", "brown", "Simon"]
		And I prepare a queryCar transaction
	 	And I evaluate the transaction with arguments ["MSPID_SCOPE_ANYFORTX"]
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

 	Scenario: Submit transaction using NETWORK_SCOPE_ALLFORTX commit handler
		Given I have a gateway as user User1 using the tls connection profile
		And I configure the gateway to use the default NETWORK_SCOPE_ALLFORTX commit handler
		And I connect the gateway
		And I use the mychannel network
		And I use the fabcar contract
		When I prepare a createCar transaction
	 	And I submit the transaction with arguments ["NETWORK_SCOPE_ALLFORTX", "Trabant", "601 Estate", "brown", "Simon"]
		And I prepare a queryCar transaction
	 	And I evaluate the transaction with arguments ["NETWORK_SCOPE_ALLFORTX"]
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

 	Scenario: Submit transaction using NETWORK_SCOPE_ANYFORTX commit handler
		Given I have a gateway as user User1 using the tls connection profile
		And I configure the gateway to use the default NETWORK_SCOPE_ANYFORTX commit handler
		And I connect the gateway
		And I use the mychannel network
		And I use the fabcar contract
		When I prepare a createCar transaction
	 	And I submit the transaction with arguments ["NETWORK_SCOPE_ANYFORTX", "Trabant", "601 Estate", "brown", "Simon"]
	 	# Can't evaluate as the peer we query may not have commited the transaction
	 	Then a response should be received
