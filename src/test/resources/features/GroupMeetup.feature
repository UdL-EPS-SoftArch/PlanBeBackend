Feature: Group Meetups
  As a group admin
  I want to create meetups that belong to my group
  So that only the active members of the group can see and join them

  Background:
    Given There is a registered user with username "owner" and password "password" and email "owner@sample.app"
    And There is a registered user with username "groupadmin" and password "password" and email "groupadmin@sample.app"
    And There is a registered user with username "member" and password "password" and email "member@sample.app"
    And There is a registered user with username "requester" and password "password" and email "requester@sample.app"
    And There is a registered user with username "outsider" and password "password" and email "outsider@sample.app"
    And There is a group with name "Gamers" with visibility "PUBLIC" owned by "owner"
    And There is a membership of "groupadmin" in the group with name "Gamers" with role "ADMIN"
    And There is a membership of "member" in the group with name "Gamers" with role "MEMBER"
    And There is a request of "requester" to join the group with name "Gamers"

  Scenario: Group admin creates a group meetup
    Given I login as "groupadmin" with password "password"
    When I create a group meetup "Game Night" for the group "Gamers"
    Then The response code is 201
    And The meetup "Game Night" belongs to the group "Gamers"

  Scenario: Plain member cannot create a group meetup
    Given I login as "member" with password "password"
    When I create a group meetup "Members Only" for the group "Gamers"
    Then The response code is 403

  Scenario: A group meetup must belong to a group
    Given I login as "groupadmin" with password "password"
    When I create a group meetup "Homeless" for no group
    Then The response code is 400

  Scenario: Active member sees the group meetup
    Given a group meetup "Game Night" exists in the group "Gamers" created by "groupadmin"
    And I login as "member" with password "password"
    When I try to retrieve the meetup "Game Night"
    Then The response code is 200

  Scenario: Active member sees the group meetup when listing
    Given a group meetup "Game Night" exists in the group "Gamers" created by "groupadmin"
    And I login as "member" with password "password"
    When I list the meetups visible to me
    Then I should see "Game Night"

  Scenario: Non-member cannot see the group meetup
    Given a group meetup "Game Night" exists in the group "Gamers" created by "groupadmin"
    And I login as "outsider" with password "password"
    When I try to retrieve the meetup "Game Night"
    Then The response code is 404

  Scenario: Non-member does not see the group meetup when listing
    Given a group meetup "Game Night" exists in the group "Gamers" created by "groupadmin"
    And I login as "outsider" with password "password"
    When I list the meetups visible to me
    Then I should not see "Game Night"

  Scenario: A pending member cannot see the group meetup
    Given a group meetup "Game Night" exists in the group "Gamers" created by "groupadmin"
    And I login as "requester" with password "password"
    When I try to retrieve the meetup "Game Night"
    Then The response code is 404

  Scenario: Active member joins the group meetup
    Given a group meetup "Game Night" exists in the group "Gamers" created by "groupadmin"
    And I login as "member" with password "password"
    When I join the group meetup "Game Night"
    Then The response code is 201
    And The participation of "member" in the meetup "Game Night" has status "CONFIRMED"

  Scenario: Non-member cannot join the group meetup
    Given a group meetup "Game Night" exists in the group "Gamers" created by "groupadmin"
    And I login as "outsider" with password "password"
    When I join the group meetup "Game Night"
    Then The join request is rejected

  Scenario: A pending member cannot join the group meetup
    Given a group meetup "Game Night" exists in the group "Gamers" created by "groupadmin"
    And I login as "requester" with password "password"
    When I join the group meetup "Game Night"
    Then The join request is rejected
