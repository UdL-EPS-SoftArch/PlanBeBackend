Feature: Manage Group Membership
    In order to manage the members of a group
    As a group owner
    I want to be able to change member roles and remove members

    Background:
        Given There is a registered user with username "user" and password "password" and email "user@sample.app"
        And There is a registered user with username "member" and password "password" and email "member@sample.app"
        And There is a registered user with username "another" and password "password" and email "another@sample.app"
        And There is a group with name "My Group" with visibility "PUBLIC" owned by "user"
        And There is a membership of "member" in the group with name "My Group" with role "MEMBER"
        And There is a membership of "another" in the group with name "My Group" with role "MEMBER"

    Scenario: Owner promotes member to ADMIN
        Given I login as "user" with password "password"
        When I change the role of the membership of "member" in the group with name "My Group" to "ADMIN"
        Then The response code is 200
        And The membership of "member" in the group with name "My Group" has role "ADMIN"

    Scenario: Member cannot change roles
        Given I login as "member" with password "password"
        When I change the role of the membership of "member" in the group with name "My Group" to "ADMIN"
        Then The response code is 403
        And The membership of "member" in the group with name "My Group" has role "MEMBER"

    Scenario: Admin removes member
        Given There is a membership of "member" in the group with name "My Group" with role "ADMIN"
        And I login as "member" with password "password"
        When I remove the membership of "another" in the group with name "My Group"
        Then The response code is 204
        And There is no membership of "another" in the group with name "My Group"

    Scenario: Admin cannot grant the OWNER role
        Given There is a membership of "member" in the group with name "My Group" with role "ADMIN"
        And I login as "member" with password "password"
        When I change the role of the membership of "another" in the group with name "My Group" to "OWNER"
        Then The response code is 403
        And The membership of "another" in the group with name "My Group" has role "MEMBER"
