Feature: Recurring Meetups
  In order to organize activities that repeat periodically
  As a meetup organizer
  I want my meetups to have a recurrence

  Background:
    Given There is a registered user with username "organizer" and password "password" and email "organizer@sample.app"

  Scenario: Organizer creates a weekly meetup
    Given I login as "organizer" with password "password"
    When I create the meetup "Padel League" starting on "2030-01-07T18:00:00Z" with recurrence "WEEKLY" until "2030-03-01"
    Then The response code is 201
    And The meetup "Padel League" has recurrence "WEEKLY" until "2030-03-01"

  Scenario: A recurring meetup may have no end
    Given I login as "organizer" with password "password"
    When I create the meetup "Monthly Book Club" starting on "2030-01-07T18:00:00Z" with recurrence "MONTHLY"
    Then The response code is 201
    And The meetup "Monthly Book Club" has recurrence "MONTHLY" and no end

  Scenario: Meetups are not recurring by default
    Given I login as "organizer" with password "password"
    When I create the meetup "One-off Dinner" starting on "2030-01-07T18:00:00Z" with recurrence ""
    Then The response code is 201
    And The meetup "One-off Dinner" has recurrence "NONE" and no end

  Scenario: Cannot create a recurring meetup that ends before it starts
    Given I login as "organizer" with password "password"
    When I create the meetup "Padel League" starting on "2030-01-07T18:00:00Z" with recurrence "WEEKLY" until "2030-01-06"
    Then The response code is 400

  Scenario: Cannot set a recurrence end on a meetup that does not repeat
    Given I login as "organizer" with password "password"
    When I create the meetup "One-off Dinner" starting on "2030-01-07T18:00:00Z" with recurrence "NONE" until "2030-03-01"
    Then The response code is 400
