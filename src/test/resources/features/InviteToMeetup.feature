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

  Scenario Outline: Cannot invite to a <status> meetup
    Given The meetup "Team Dinner" has status "<status>"
    And I login as "organizer" with password "password"
    When I invite "guest" to the meetup "Team Dinner"
    Then The response code is 409
    And There is no invitation of "guest" to the meetup "Team Dinner"

    Examples:
      | status    |
      | CANCELLED |
      | FINISHED  |

  Scenario: Invitee accepts an invitation
    Given "guest" has already been invited to the meetup "Team Dinner"
    And I login as "guest" with password "password"
    When I answer the invitation of "guest" to the meetup "Team Dinner" with "ACCEPTED"
    Then The response code is 200
    And The invitation of "guest" to the meetup "Team Dinner" has status "ACCEPTED"
    And "guest" has a participation in the meetup "Team Dinner" with status "CONFIRMED"

  Scenario: Invitee accepts an invitation to a full meetup
    Given There is a meetup with title "Small Dinner" organized by "organizer" with capacity 1
    And "another" is a confirmed participant of the meetup "Small Dinner"
    And "guest" has already been invited to the meetup "Small Dinner"
    And I login as "guest" with password "password"
    When I answer the invitation of "guest" to the meetup "Small Dinner" with "ACCEPTED"
    Then The response code is 200
    And "guest" has a participation in the meetup "Small Dinner" with status "WAITING"

  Scenario Outline: Cannot accept an invitation to a <status> meetup
    Given "guest" has already been invited to the meetup "Team Dinner"
    And The meetup "Team Dinner" has status "<status>"
    And I login as "guest" with password "password"
    When I answer the invitation of "guest" to the meetup "Team Dinner" with "ACCEPTED"
    Then The response code is 409
    And The invitation of "guest" to the meetup "Team Dinner" has status "PENDING"
    And "guest" has no participation in the meetup "Team Dinner"

    Examples:
      | status    |
      | CANCELLED |
      | FINISHED  |

  Scenario: Invitee declines an invitation
    Given "guest" has already been invited to the meetup "Team Dinner"
    And I login as "guest" with password "password"
    When I answer the invitation of "guest" to the meetup "Team Dinner" with "DECLINED"
    Then The response code is 200
    And The invitation of "guest" to the meetup "Team Dinner" has status "DECLINED"
    And "guest" has no participation in the meetup "Team Dinner"

  Scenario: Organizer cannot answer on behalf of the invitee
    Given "guest" has already been invited to the meetup "Team Dinner"
    And I login as "organizer" with password "password"
    When I answer the invitation of "guest" to the meetup "Team Dinner" with "ACCEPTED"
    Then The response code is 403
    And The invitation of "guest" to the meetup "Team Dinner" has status "PENDING"

  Scenario: Unrelated user cannot answer an invitation
    Given "guest" has already been invited to the meetup "Team Dinner"
    And I login as "another" with password "password"
    When I answer the invitation of "guest" to the meetup "Team Dinner" with "ACCEPTED"
    Then The response code is 404
    And The invitation of "guest" to the meetup "Team Dinner" has status "PENDING"

  Scenario: Cannot answer an invitation twice
    Given "guest" has already been invited to the meetup "Team Dinner"
    And The invitation of "guest" to the meetup "Team Dinner" was already answered with "DECLINED"
    And I login as "guest" with password "password"
    When I answer the invitation of "guest" to the meetup "Team Dinner" with "ACCEPTED"
    Then The response code is 409
    And The invitation of "guest" to the meetup "Team Dinner" has status "DECLINED"

  Scenario: Cannot move an invitation to another meetup
    Given There is a meetup with title "Small Dinner" organized by "organizer" with capacity 1
    And "guest" has already been invited to the meetup "Team Dinner"
    And I login as "guest" with password "password"
    When I move the invitation of "guest" to the meetup "Team Dinner" to the meetup "Small Dinner"
    Then The response code is 400
    And There is no invitation of "guest" to the meetup "Small Dinner"
