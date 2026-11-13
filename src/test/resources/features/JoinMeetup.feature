Feature: Join and Leave a Public Meetup

  As a user, I want to join and leave public meetups so that I can participate in activities I'm interested in.

  Background:
    Given the following meetups exist:
      | title        | capacity | visibility | status |
      | Padel Match  | 4        | PUBLIC     | OPEN   |
      | Secret Club  | 10       | PRIVATE    | OPEN   |
      | Full Event   | 1        | PUBLIC     | OPEN   |

  Scenario: User joins public meetup successfully
    Given I login as "user1" with password "password123"
    When I join the meetup "Padel Match"
    Then the response code is 201
    And the participation status is "CONFIRMED"

  Scenario: User joins full public meetup (Waiting List)
    Given I login as "user1" with password "password123"
    And the meetup "Full Event" is already full
    When I join the meetup "Full Event"
    Then the response code is 201
    And the participation status is "WAITING" 

  Scenario: User cannot join twice
    Given I login as "user1" with password "password123"
    And I have already joined the meetup "Padel Match"
    When I join the meetup "Padel Match"
    Then the response code is 409

  Scenario: User leaves a meetup
    Given I login as "user1" with password "password123"
    And I have joined the meetup "Padel Match"
    When I leave the meetup "Padel Match"
    Then the response code is 204
    And the participation status is "CANCELLED"

  Scenario: User who left can join again
    Given I login as "user1" with password "password123"
    And I have joined the meetup "Padel Match"
    When I leave the meetup "Padel Match"
    Then the response code is 204
    When I join the meetup "Padel Match"
    Then the response code is 201
    And the participation status is "CONFIRMED"
    And my earlier participation in the meetup "Padel Match" is kept as "CANCELLED"

  Scenario: Cannot join private meetup without invitation
    Given I login as "user1" with password "password123"
    When I join the meetup "Secret Club"
    Then the response code is 403

  Scenario: Cancel meetup → participations CANCELLED
    Given I login as "user1" with password "password123"
    And I have joined the meetup "Padel Match"
    And I login as "organizer" with password "password"
    When the organizer cancels the meetup "Padel Match"
    Then the response code is 204
    And the participation status is "CANCELLED"

  Scenario: Cannot join a CANCELLED meetup
    Given I login as "user1" with password "password123"
    And the meetup "Padel Match" is cancelled
    When I join the meetup "Padel Match"
    Then the response code is 409
