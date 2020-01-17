#
# Copyright 2019 IBM All Rights Reserved.
#
# SPDX-License-Identifier: Apache-2.0
#
Feature: CouchDB wallet
	Background:
		Given I use a CouchDB wallet

 	Scenario: List empty wallet
	 	Then the wallet should contain 0 identities

 	Scenario: Put an identity
		When I put an identity named "penguin" into the wallet
	 	Then the wallet should contain an identity named "penguin"

	Scenario: Update an identity
		When I put an identity named "penguin" into the wallet
		And I put an identity named "penguin" into the wallet
		Then the wallet should contain 1 identities

	Scenario: Remove an identity
		When I put an identity named "penguin" into the wallet
		And I remove an identity named "penguin" from the wallet
		Then the wallet should contain 0 identities

	Scenario: Remove an identity that does not exist
		When I remove an identity named "penguin" from the wallet
		Then the wallet should contain 0 identities

	Scenario: Get an identity
		When I put an identity named "penguin" into the wallet
		Then I should be able to get an identity named "penguin" from the wallet

	Scenario: Get a removed identity
		When I put an identity named "penguin" into the wallet
		And I remove an identity named "penguin" from the wallet
		Then I should not be able to get an identity named "penguin" from the wallet
