Feature: Join Meetup
  As a user, I want to join a meetup so that I can participate in the activity.

  Scenario: Last spot fills meetup
    Given a meetup exists with capacity 2
    And 1 user has already joined the meetup
    When I join the meetup
    Then the response code is 201
    And the meetup status should be "FULL"

  Scenario: Join full meetup puts user on waiting list
    Given a meetup exists with capacity 2
    And 2 users have already joined the meetup
    When I join the meetup
    Then the response code is 201


  Scenario: Someone leaves full meetup opens it
    Given a meetup exists with capacity 2
    And 2 users have already joined the meetup
    When a participant leaves the meetup
    Then the response code is 204
    And the meetup status should be "OPEN"
