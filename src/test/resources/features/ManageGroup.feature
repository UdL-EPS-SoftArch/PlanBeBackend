Feature: Manage Group
    In order to organize people into groups
    As a user
    I want to be able to create, retrieve, edit and delete groups

    Background:
        Given There is a registered user with username "user" and password "password" and email "user@sample.app"
        And There is a registered user with username "another" and password "password" and email "another@sample.app"
        And There is a group with name "Existing Group" with visibility "PUBLIC" owned by "user"

    Scenario: The creator of a group becomes its OWNER
        Given I login as "user" with password "password"
        When I create a new group with name "My Group"
        Then The response code is 201
        And The new group is owned by "user"

    Scenario: Cannot create a group with empty name
        Given I login as "user" with password "password"
        When I create a new group with name ""
        Then The response code is 400
        And The error message is "must not be blank"

    Scenario: The owner can edit the group
        Given I login as "user" with password "password"
        When I edit the group with name "Existing Group" to have name "Edited Group"
        Then The response code is 200
        And The list of groups owned by "user" includes 1 named "Edited Group"
        And The list of groups owned by "user" includes 0 named "Existing Group"

    Scenario: Only OWNER or ADMIN can edit the group
        Given I login as "another" with password "password"
        When I edit the group with name "Existing Group" to have name "Edited Group"
        Then The response code is 403
        And The list of groups owned by "user" includes 1 named "Existing Group"
        And The list of groups owned by "user" includes 0 named "Edited Group"

    Scenario: The owner can delete the group
        Given I login as "user" with password "password"
        When I delete the group with name "Existing Group"
        Then The response code is 204
        And The list of groups owned by "user" includes 0 named "Existing Group"

    Scenario: Only OWNER deletes the group
        Given I login as "another" with password "password"
        When I delete the group with name "Existing Group"
        Then The response code is 403
        And The list of groups owned by "user" includes 1 named "Existing Group"

    Scenario: A user can retrieve a public group of another user
        Given I login as "another" with password "password"
        When I retrieve the group with name "Existing Group"
        Then The response code is 200

    Scenario: Cannot retrieve a private group owned by another user
        Given There is a group with name "Private Group" with visibility "PRIVATE" owned by "user"
        And I login as "another" with password "password"
        When I retrieve the group with name "Private Group"
        Then The response code is 404
