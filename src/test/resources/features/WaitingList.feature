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

  Scenario: Last spot fills meetup
    Given a meetup exists with capacity 2
    And 1 user has already joined the meetup
    When I join the meetup
    Then the response code is 201
    And the meetup status should be "FULL"

  Scenario: Someone leaves full meetup opens it
    Given a meetup exists with capacity 2
    And 2 users have already joined the meetup
    When a participant leaves the meetup
    Then the response code is 204
    And the meetup status should be "OPEN"

  Scenario: Simultaneous joins never overbook the meetup
    Given There is a registered user with username "organizer" and password "password" and email "organizer@sample.app"
    And There is a public meetup with title "Padel" organized by "organizer" and capacity 2
    When 8 users join the meetup "Padel" at the same time
    Then The meetup "Padel" has 2 confirmed and 6 waiting participants
    And The meetup "Padel" has status "FULL" and capacity 2

  Scenario: The same user joining twice at the same time gets only one spot
    Given There is a registered user with username "organizer" and password "password" and email "organizer@sample.app"
    And There is a public meetup with title "Padel" organized by "organizer" and capacity 2
    When "racer0" joins the meetup "Padel" 6 times at the same time
    Then Exactly 1 of those requests is answered 201 and the rest 409
    And The meetup "Padel" has 1 confirmed and 0 waiting participants
    And The meetup "Padel" has status "OPEN" and capacity 2

  Scenario: Waiting user cannot promote themselves
    Given There is a registered user with username "organizer" and password "password" and email "organizer@sample.app"
    And There is a registered user with username "waiter" and password "password" and email "waiter@sample.app"
    And There is a public meetup with title "Padel" organized by "organizer" and capacity 1
    And "organizer" participates in the meetup "Padel" with status "CONFIRMED"
    And "waiter" participates in the meetup "Padel" with status "WAITING"
    And I login as "waiter" with password "password"
    When I set my participation in the meetup "Padel" to status "CONFIRMED"
    Then The response code is 403
    And The participation of "waiter" in the meetup "Padel" has status "WAITING"

  Scenario: A meetup cancelled while its waiting list is being filled promotes nobody
    Given There is a registered user with username "organizer" and password "password" and email "organizer@sample.app"
    And There is a registered user with username "waiter" and password "password" and email "waiter@sample.app"
    And There is a public meetup with title "Padel" organized by "organizer" and capacity 2
    And "waiter" participates in the meetup "Padel" with status "WAITING"
    And The meetup "Padel" is in status "OPEN"
    When The meetup "Padel" is cancelled by another request while its waiting list is being filled
    Then The participation of "waiter" in the meetup "Padel" has status "WAITING"
    And User "waiter" has no notifications
    And The meetup "Padel" has status "CANCELLED" and capacity 2
