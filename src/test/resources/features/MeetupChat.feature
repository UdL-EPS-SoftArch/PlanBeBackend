Feature: Meetup chat
  In order to coordinate with the people attending a meetup
  As a confirmed participant
  I want to read and send messages in the meetup chat

  Background:
    Given There is a registered user with username "organizer" and password "password" and email "organizer@sample.app"
    And There is a registered user with username "alice" and password "password" and email "alice@sample.app"
    And There is a registered user with username "outsider" and password "password" and email "outsider@sample.app"

  Scenario: Participant sends a message
    Given a meetup "Hiking Trip" exists created by "organizer" with visibility "PUBLIC"
    And "alice" is a confirmed participant of the meetup "Hiking Trip"
    And I login as "alice" with password "password"
    When I send the chat message "See you at the trailhead" to the meetup "Hiking Trip"
    Then The response code is 201
    And The chat message "See you at the trailhead" of the meetup "Hiking Trip" was sent by "alice"

  Scenario: Non-participant cannot read a message
    Given a meetup "Hiking Trip" exists created by "organizer" with visibility "PUBLIC"
    And "alice" is a confirmed participant of the meetup "Hiking Trip"
    And "alice" has already sent the chat message "See you at the trailhead" to the meetup "Hiking Trip"
    And I login as "outsider" with password "password"
    When I retrieve the chat message "See you at the trailhead" of the meetup "Hiking Trip"
    Then The response code is 404

  Scenario: Messages are ordered by sentAt
    Given a meetup "Hiking Trip" exists created by "organizer" with visibility "PUBLIC"
    And "alice" is a confirmed participant of the meetup "Hiking Trip"
    And the meetup "Hiking Trip" has chat messages sent by "alice":
      | text   | sentAt               |
      | Second | 2026-10-23T10:00:02Z |
      | First  | 2026-10-23T10:00:01Z |
      | Third  | 2026-10-23T10:00:03Z |
    And I login as "alice" with password "password"
    When I list the chat messages of the meetup "Hiking Trip"
    Then The chat messages are ordered "First", "Second", "Third"
