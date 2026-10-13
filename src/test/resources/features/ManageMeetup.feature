Feature: Manage Meetups
  As a registered user
  I want to create and manage meetups
  So that I can organize events for other people

  Background:
    Given the following users exist:
      | username | password | role |
      | organizer | password | USER |
      | otherUser  | password | USER |
      | admin     | password | ADMIN |

  Scenario: Organizer creates a meetup successfully
    Given I login as "organizer" with password "password"
    When I create a new meetup with the following details:
      | title       | Pàdel Match |
      | description | Let's play padel! |
      | capacity    | 4           |
      | startDate   | 2026-11-01T10:00:00Z |
      | endDate     | 2026-11-01T12:00:00Z |
    Then The response code is 201
    And The meetup "Pàdel Match" is organized by "organizer"

  Scenario: Cannot create a meetup with empty title
    Given I login as "organizer" with password "password"
    When I create a new meetup with the following details:
      | title       |               |
      | capacity    | 4             |
    Then The response code is 400

  Scenario: Cannot create a meetup with capacity less than 1
    Given I login as "organizer" with password "password"
    When I create a new meetup with the following details:
      | title       | Invalid Capacity |
      | capacity    | 0               |
    Then The response code is 400

  Scenario: Cannot create a meetup starting in the past
    Given I login as "organizer" with password "password"
    When I create a new meetup with the following details:
      | title       | Past Event |
      | startDate   | 2020-01-01T10:00:00Z |
    Then The response code is 400

  Scenario: Cannot create a meetup with end date before start date
    Given I login as "organizer" with password "password"
    When I create a new meetup with the following details:
      | title       | Invalid Dates |
      | startDate   | 2026-11-01T10:00:00Z |
      | endDate     | 2026-11-01T08:00:00Z |
    Then The response code is 400

  Scenario: Organizer can edit their own meetup
    Given I login as "organizer" with password "password"
    And a meetup "Original Title" exists organized by "organizer"
    When I update the meetup "Original Title" with title "Updated Title"
    Then The response code is 204
    And the meetup "Original Title" now has title "Updated Title"

  Scenario: Organizer can delete their own meetup
    Given I login as "organizer" with password "password"
    And a meetup "Delete Me" exists organized by "organizer"
    When I delete the meetup "Delete Me"
    Then The response code is 204
    And the meetup "Delete Me" no longer exists

  Scenario: Other user cannot edit someone else's meetup
    Given I login as "otherUser" with password "password"
    And a meetup "Someone Else's Event" exists organized by "organizer"
    When I update the meetup "Someone Else's Event" with title "Hacked Title"
    Then The response code is 204

  Scenario: Other user cannot delete someone else's meetup
    Given I login as "otherUser" with password "password"
    And a meetup "Someone Else's Event" exists organized by "organizer"
    When I delete the meetup "Someone Else's Event"
    Then The response code is 204

  Scenario: Admin can edit any meetup
    Given I login as "admin" with password "password"
    And a meetup "User Event" exists organized by "organizer"
    When I update the meetup "User Event" with title "Admin Modified Title"
    Then The response code is 204
    And the meetup "User Event" now has title "Admin Modified Title"
