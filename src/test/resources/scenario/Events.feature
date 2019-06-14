#
# Copyright 2019 IBM All Rights Reserved.
#
# SPDX-License-Identifier: Apache-2.0
#
Feature: Event listening
	Background:
		Given I have deployed a tls Fabric network
		And I have created and joined all channels from the tls connection profile
		And I deploy node chaincode named fabcar at version 1.0.0 for all organizations on channel mychannel with endorsement policy 1AdminOr2Other and arguments ["initLedger"]

 	Scenario: Receive block event following submit transaction
 	    Given I have a gateway as user User1 using the tls connection profile
		And I connect the gateway
		And I use the mychannel network
		And I use the fabcar contract
		When I add a block listener
		And I prepare a createCar transaction
	 	And I submit the transaction with arguments ["block_event_test", "Volvo", "XC40", "black", "Simon"]
	 	Then a block event should be received

 	Scenario: Receive contract event emitted by a transaction
 	    Given I have a gateway as user User1 using the tls connection profile
		And I connect the gateway
		And I use the mychannel network
		And I use the fabcar contract
		When I add a contract listener for events matching "createCar"
		And I prepare a createCar transaction
	 	And I submit the transaction with arguments ["contract_event_test", "Volvo", "XC40", "black", "Simon"]
	 	Then a contract event with payload "contract_event_test" should be received

	Scenario: Replay of block events
		Given I have a gateway as user User1 using the tls connection profile
		And I connect the gateway
		And I use the mychannel network
		And I use the fabcar contract
		When I prepare a createCar transaction
		And I submit the transaction with arguments ["block_replay", "Volvo", "XC40", "black", "Simon"]
		And I add a block listener with replay from block 1
		Then a block event should be received

	Scenario: Checkpoint replay of block events
 	    Given I have a gateway as user User1 using the tls connection profile
		And I connect the gateway
		And I use the mychannel network
		And I use the fabcar contract
		When I add a block listener with a file checkpointer
		And I prepare a createCar transaction
	 	And I submit the transaction with arguments ["block_checkpoint1", "Volvo", "XC40", "black", "Simon"]
        And I wait for a block event to be received
	 	And I remove the block listener
		And I prepare a createCar transaction
	 	And I submit the transaction with arguments ["block_checkpoint2", "Volvo", "XC40", "black", "Simon"]
		And I add a block listener with a file checkpointer
	 	Then a block event should be received

	Scenario: Replay of contract events
		Given I have a gateway as user User1 using the tls connection profile
		And I connect the gateway
		And I use the mychannel network
		And I use the fabcar contract
		When I prepare a createCar transaction
		And I submit the transaction with arguments ["contract_replay", "Volvo", "XC40", "black", "Simon"]
		And I add a contract listener for events matching "createCar" with replay from block 1
		Then a contract event with payload "contract_replay" should be received

	Scenario: Checkpoint replay of contract events
		Given I have a gateway as user User1 using the tls connection profile
		And I connect the gateway
		And I use the mychannel network
		And I use the fabcar contract
		When I add a contract listener for events matching "createCar" with a file checkpointer
		And I prepare a createCar transaction
		And I submit the transaction with arguments ["contract_checkpoint1", "Volvo", "XC40", "black", "Simon"]
		And I wait for a contract event with payload "contract_checkpoint1" to be received
		And I remove the contract listener
		And I prepare a createCar transaction
		And I submit the transaction with arguments ["contract_checkpoint2", "Volvo", "XC40", "black", "Simon"]
		And I add a contract listener for events matching "createCar" with a file checkpointer
		Then a contract event with payload "contract_checkpoint2" should be received
