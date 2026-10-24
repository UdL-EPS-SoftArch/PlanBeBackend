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

  Scenario: Finishing a weekly meetup creates the next occurrence 7 days later
    Given The meetup "Padel League" organized by "organizer" starts in 2 days and repeats "WEEKLY"
    And There is a registered user with username "player" and password "password" and email "player@sample.app"
    And "player" is a confirmed participant of the meetup "Padel League"
    And I login as "organizer" with password "password"
    When I finish the meetup "Padel League"
    Then The response code is 204
    And The meetup "Padel League" has a next occurrence starting 7 days after it
    And The next occurrence of "Padel League" is an open copy without participants

  Scenario: Finishing a biweekly meetup creates the next occurrence 14 days later
    Given The meetup "Chess Club" organized by "organizer" starts in 2 days and repeats "BIWEEKLY"
    And I login as "organizer" with password "password"
    When I finish the meetup "Chess Club"
    Then The meetup "Chess Club" has a next occurrence starting 14 days after it

  Scenario: Finishing a monthly meetup creates the next occurrence one calendar month later
    Given The meetup "Book Club" organized by "organizer" starts in 2 days and repeats "MONTHLY"
    And I login as "organizer" with password "password"
    When I finish the meetup "Book Club"
    Then The meetup "Book Club" has a next occurrence starting 1 month after it

  Scenario: The occurrence starting on the recurrence end is still created
    Given The meetup "Padel League" organized by "organizer" starts in 2 days and repeats "WEEKLY" until 9 days from now
    And I login as "organizer" with password "password"
    When I finish the meetup "Padel League"
    Then The meetup "Padel League" has a next occurrence starting 7 days after it

  Scenario: No new occurrence after the recurrence end
    Given The meetup "Padel League" organized by "organizer" starts in 2 days and repeats "WEEKLY" until 8 days from now
    And I login as "organizer" with password "password"
    When I finish the meetup "Padel League"
    Then The response code is 204
    And The meetup "Padel League" has no next occurrence

  Scenario: Finishing a meetup late skips to the first future occurrence
    Given The meetup "Padel League" organized by "organizer" starts in -15 days and repeats "WEEKLY"
    And I login as "organizer" with password "password"
    When I finish the meetup "Padel League"
    Then The response code is 204
    And The meetup "Padel League" has a next occurrence starting 21 days after it

  Scenario: Finishing a meetup again does not duplicate the next occurrence
    Given The meetup "Padel League" organized by "organizer" starts in 2 days and repeats "WEEKLY"
    And I login as "organizer" with password "password"
    And I finish the meetup "Padel League"
    And I set the status of the meetup "Padel League" to "OPEN"
    When I finish the meetup "Padel League"
    Then The meetup "Padel League" has exactly one next occurrence

  Scenario: Cancelling or finishing a non-recurring meetup creates no occurrence
    Given The meetup "Padel League" organized by "organizer" starts in 2 days and repeats "WEEKLY"
    And The meetup "One-off Dinner" organized by "organizer" starts in 2 days and repeats "NONE"
    And I login as "organizer" with password "password"
    When I set the status of the meetup "Padel League" to "CANCELLED"
    And I finish the meetup "One-off Dinner"
    Then The meetup "Padel League" has no next occurrence
    And The meetup "One-off Dinner" has no next occurrence

  Scenario: The previous occurrence cannot be set by the client
    Given The meetup "Padel League" organized by "organizer" starts in 2 days and repeats "WEEKLY"
    And I login as "organizer" with password "password"
    When I create the meetup "Fake Next" as the next occurrence of "Padel League"
    Then The response code is 201
    And The meetup "Padel League" has no next occurrence

  Scenario: The previous occurrence cannot be changed by the client
    Given The meetup "Padel League" organized by "organizer" starts in 2 days and repeats "WEEKLY"
    And The meetup "Other Meetup" organized by "organizer" starts in 9 days and repeats "NONE"
    And I login as "organizer" with password "password"
    When I set the previous occurrence of "Other Meetup" to "Padel League"
    Then The meetup "Padel League" has no next occurrence
