Feature: Invite to Meetup
  In order to bring people to my meetups
  As a meetup organizer
  I want to be able to invite other users to a meetup

  Background:
    Given There is a registered user with username "organizer" and password "password" and email "organizer@sample.app"
    And There is a registered user with username "guest" and password "password" and email "guest@sample.app"
    And There is a registered user with username "another" and password "password" and email "another@sample.app"
    And There is a meetup with title "Team Dinner" organized by "organizer"

  Scenario: Organizer invites a user
    Given I login as "organizer" with password "password"
    When I invite "guest" to the meetup "Team Dinner"
    Then The response code is 201
    And The invitation of "guest" to the meetup "Team Dinner" has status "PENDING"
    And The invitation of "guest" to the meetup "Team Dinner" was sent by "organizer"

  Scenario: Cannot invite the same user twice
    Given "guest" has already been invited to the meetup "Team Dinner"
    And I login as "organizer" with password "password"
    When I invite "guest" to the meetup "Team Dinner"
    Then The response code is 409

  Scenario: Non-organizer cannot invite
    Given I login as "another" with password "password"
    When I invite "guest" to the meetup "Team Dinner"
    Then The response code is 403
    And There is no invitation of "guest" to the meetup "Team Dinner"

  Scenario: Cannot invite when not logged in
    Given I'm not logged in
    When I invite "guest" to the meetup "Team Dinner"
    Then The response code is 401
    And There is no invitation of "guest" to the meetup "Team Dinner"
