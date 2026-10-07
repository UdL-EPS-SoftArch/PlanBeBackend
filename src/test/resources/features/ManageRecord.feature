Feature: Manage Record
    In order to manage user records
    As a user
    I want to be able to create, retrieve, edit and delete my records

    Background:
        Given There is a registered user with username "user" and password "password" and email "user@sample.app"
        And There is a registered user with username "another" and password "password" and email "another@sample.app"
        And There is a record with name "Existing Record" owned by "user"

    Scenario: The creator of a record owns it
        Given I login as "user" with password "password"
        When I create a new record with name "My Record"
        Then The response code is 201
        And The new record is owned by "user"
        And The list of records owned by "user" includes 1 named "My Record"

    Scenario: Cannot create a record with empty name
        Given I login as "user" with password "password"
        When I create a new record with name ""
        Then The response code is 400
        And The error message is "must not be blank"

    Scenario: Creators can retrieve their own records
        Given I login as "user" with password "password"
        When I retrieve the record with name "Existing Record"
        Then The response code is 200
        And The retrieved record has name "Existing Record"

    Scenario: Cannot retrieve a record owned by another user and by default private
        Given I login as "another" with password "password"
        When I retrieve the record with name "Existing Record"
        Then The response code is 404

    Scenario: Can retrieve if the other user makes it public
        Given I login as "user" with password "password"
        And I make the record with name "Existing Record" public
        And I login as "another" with password "password"
        When I retrieve the record with name "Existing Record"
        Then The response code is 200

    Scenario: Creator can edit and modified timestamp is updated
        Given I login as "user" with password "password"
        When I edit the record with name "Existing Record" to have name "Edited Record"
        Then The response code is 200
        And The list of records owned by "user" includes 1 named "Edited Record"
        And The list of records owned by "user" includes 0 named "Existing Record"
        And The modified timestamp of the record with name "Edited Record" is after the created one

    Scenario: Cannot edit a record owned by another user
        Given I login as "another" with password "password"
        When I edit the record with name "Existing Record" to have name "Edited Record"
        Then The response code is 404
    
    Scenario: Creator can delete their own record
        Given I login as "user" with password "password"
        When I delete the record with name "Existing Record"
        Then The response code is 204
        And The list of records owned by "user" includes 0 named "Existing Record"

    Scenario: Cannot delete a record owned by another user
        Given I login as "another" with password "password"
        When I delete the record with name "Existing Record"
        Then The response code is 404