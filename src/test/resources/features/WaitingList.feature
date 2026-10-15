Feature: Waiting List
  As a user, I want to be put on a waiting list when a meetup is full, so that I can be notified if a spot becomes available.

  Scenario: Join full meetup puts user on waiting list
    Given a meetup exists with capacity 1
    And 1 user has already joined the meetup
    When I join the meetup
    Then the response code is 201
    And my participation status should be "WAITING"

  Scenario: Confirmed user leaves promotes first waiting user
    Given a meetup exists with capacity 1
    And 1 user has already joined the meetup
    And another user is on the waiting list
    When a participant leaves the meetup
    Then the response code is 204
    And the waiting user status should be "CONFIRMED"
    And the meetup status should be "FULL"
