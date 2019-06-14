#
# Copyright 2019 IBM All Rights Reserved.
#
# SPDX-License-Identifier: Apache-2.0
#
Feature: Configure Fabric using SDK using discovery service and submit/evaluate using a network Gateway

	Background:
		Given I have deployed a tls Fabric network
		And I have created and joined all channels from the tls connection profile
#		And I update channel with name mychannel with config file mychannel-org1anchor.tx from the tls connection profile
		And I deploy node chaincode named marbles0 at version 1.0.0 for all organizations on channel mychannel with endorsement policy 1AdminOr2Other and arguments ["init", "a", "1000", "b", "2000"]

 	Scenario: Using a Gateway with discovery I can submit and evaluate transactions on instantiated node chaincode
		Given I have a gateway as user User1 using the discovery connection profile
		And I connect the gateway
		And I use the mychannel network
		And I use the marbles0 contract
	 	When I prepare an initMarble transaction
	 	And I submit the transaction with arguments ["marble1", "blue", "50", "bob"]
	 	And I prepare a readMarble transaction
	 	And I evaluate the transaction with arguments ["marble1"]
	 	Then the response should be JSON matching
		"""
		{
			"color":"blue",
			"docType":"marble",
			"name":"marble1",
			"owner":"bob",
			"size":50
		}
		"""
