Feature: Meetup Status Management
  As an organizer
  I want to manage the status of my meetups
  So that users know if a meetup is open, full, or cancelled

  Scenario: New meetup is OPEN by default
    Given I'm logged in as "organizer"
    When I create a meetup with title "Default Status Test"
    Then the meetup "Default Status Test" should have status "OPEN"

  Scenario: Organizer can cancel a meetup
    Given I'm logged in as "organizer"
    And a meetup "Cancellable Meetup" exists
    When I update the meetup "Cancellable Meetup" status to "CANCELLED"
    Then the meetup "Cancellable Meetup" should have status "CANCELLED"

  Scenario: Non-organizer cannot cancel a meetup
    Given I'm logged in as "user"
    And a meetup "Protected Meetup" exists created by "organizer"
    When I update the meetup "Protected Meetup" status to "CANCELLED"
    Then the response code is 403

  Scenario: Cannot reopen a FINISHED meetup
    Given I'm logged in as "organizer"
    And a meetup "Finished Meetup" exists with status "FINISHED"
    When I update the meetup "Finished Meetup" status to "OPEN"
    Then the response code is 204
