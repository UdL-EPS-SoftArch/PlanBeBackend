Feature: Manage Group Membership
    In order to manage the members of a group
    As a group owner
    I want to be able to change member roles and remove members

    Background:
        Given There is a registered user with username "user" and password "password" and email "user@sample.app"
        And There is a registered user with username "member" and password "password" and email "member@sample.app"
        And There is a registered user with username "another" and password "password" and email "another@sample.app"
        And There is a registered user with username "requester" and password "password" and email "requester@sample.app"
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

    Scenario: A user requests to join a public group
        Given There is a group with name "Open Group" with visibility "PUBLIC" owned by "user"
        And I login as "requester" with password "password"
        When I request to join the group with name "Open Group"
        Then The response code is 201
        And The membership of "requester" in the group with name "Open Group" has status "PENDING"
        And The membership of "requester" in the group with name "Open Group" has role "MEMBER"

    Scenario: A join request cannot choose its own role or status
        Given There is a group with name "Forced Group" with visibility "PUBLIC" owned by "user"
        And I login as "requester" with password "password"
        When I request to join the group with name "Forced Group" with role "ADMIN" and status "ACTIVE"
        Then The response code is 201
        And The membership of "requester" in the group with name "Forced Group" has status "PENDING"
        And The membership of "requester" in the group with name "Forced Group" has role "MEMBER"

    Scenario: A group admin approves a join request
        Given There is a group with name "Approval Group" with visibility "PUBLIC" owned by "user"
        And There is a membership of "member" in the group with name "Approval Group" with role "ADMIN"
        And There is a request of "requester" to join the group with name "Approval Group"
        And I login as "member" with password "password"
        When I approve the join request of "requester" in the group with name "Approval Group"
        Then The response code is 200
        And The membership of "requester" in the group with name "Approval Group" has status "ACTIVE"

    Scenario: A member cannot approve their own join request
        Given There is a group with name "Member Approval Group" with visibility "PUBLIC" owned by "user"
        And There is a request of "requester" to join the group with name "Member Approval Group"
        And I login as "requester" with password "password"
        When I approve the join request of "requester" in the group with name "Member Approval Group"
        Then The response code is 403
        And The membership of "requester" in the group with name "Member Approval Group" has status "PENDING"

    Scenario: The owner can also approve a join request
        Given There is a group with name "Owner Approval Group" with visibility "PUBLIC" owned by "user"
        And There is a request of "requester" to join the group with name "Owner Approval Group"
        And I login as "user" with password "password"
        When I approve the join request of "requester" in the group with name "Owner Approval Group"
        Then The response code is 200
        And The membership of "requester" in the group with name "Owner Approval Group" has status "ACTIVE"
