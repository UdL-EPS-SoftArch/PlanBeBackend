Feature: API security
  As the platform
  I want every write to need a logged-in user and every resource to follow its owner's privacy
  So that nobody reads or changes what is not theirs

  Background:
    Given There is a registered user with username "organizer" and password "password" and email "organizer@sample.app"
    And There is a registered user with username "participant" and password "password" and email "participant@sample.app"
    And There is a registered user with username "other" and password "password" and email "other@sample.app"
    And There is a registered user with username "outsider" and password "password" and email "outsider@sample.app"
    And There is a registered admin with username "admin" and password "password" and email "admin@sample.app"
    And There is a "FINISHED" meetup "Padel Match" organized by "organizer" with a confirmed participant "participant"
    And "other" is a confirmed participant of the meetup "Padel Match"

  # #88 Authentication on every write and association

  Scenario Outline: An anonymous user cannot create <resource>
    Given I'm not logged in
    When I send an empty POST request to "/<resource>"
    Then The response code is 401

    Examples:
      | resource       |
      | groups         |
      | meetups        |
      | participations |
      | reviews        |
      | memberships    |
      | comments       |
      | chatMessages   |

  Scenario: An anonymous user cannot read the participant of a participation
    Given I'm not logged in
    When I request the participant of the participation of "participant" in the meetup "Padel Match"
    Then The response code is 401

  Scenario: Anonymous registration keeps working
    Given I'm not logged in
    When I register a new user with username "newcomer", email "newcomer@sample.app" and password "password"
    Then The response code is 201

  # #89 Participations

  Scenario: An anonymous user cannot list participations
    Given I'm not logged in
    When I list the participations
    Then The response code is 401

  Scenario: A user only lists their own participations
    Given I login as "participant" with password "password"
    When I list the participations
    Then The response code is 200
    And The response contains 1 participation of "participant"

  Scenario: An unrelated user cannot read or look up another user's participation
    Given I login as "outsider" with password "password"
    When I retrieve the participation of "participant" in the meetup "Padel Match"
    Then The response code is 404
    When I look up the participation of "participant" in the meetup "Padel Match" through the search endpoint
    Then The response code is 404

  Scenario: The organizer sees the participations of their meetup
    Given I login as "organizer" with password "password"
    When I list the participations
    Then The response code is 200
    And The response contains 2 participations

  Scenario: Admin sees any participation
    Given I login as "admin" with password "password"
    When I retrieve the participation of "participant" in the meetup "Padel Match"
    Then The response code is 200

  # #90 Reviews

  Scenario: Reviews of a private meetup are hidden from anonymous and unrelated users
    Given The meetup "Padel Match" has visibility "PRIVATE"
    And "participant" has already reviewed the meetup "Padel Match" with rating 5
    And I'm not logged in
    When I list the reviews of the meetup "Padel Match"
    Then The response contains 0 reviews
    Given I login as "outsider" with password "password"
    When I list the reviews of the meetup "Padel Match"
    Then The response contains 0 reviews
    When I retrieve the review of "participant" on the meetup "Padel Match"
    Then The response code is 404
    When I look up the review of "participant" on the meetup "Padel Match" through the search endpoint
    Then The response code is 404

  Scenario: Participants of a private meetup see its reviews
    Given The meetup "Padel Match" has visibility "PRIVATE"
    And "participant" has already reviewed the meetup "Padel Match" with rating 5
    And I login as "other" with password "password"
    When I list the reviews of the meetup "Padel Match"
    Then The response contains 1 review

  Scenario: Reviews of a public meetup remain visible
    Given "participant" has already reviewed the meetup "Padel Match" with rating 5
    And I'm not logged in
    When I list the reviews of the meetup "Padel Match"
    Then The response contains 1 review

  # #91 Internal repository methods

  Scenario Outline: The internal meetup query <query> is not exposed
    Given I login as "organizer" with password "password"
    When I send a GET request to "/meetups/search/<query>"
    Then The response code is 404

    Examples:
      | query            |
      | updateStatus     |
      | findByIdAndOwner |

  Scenario: Searching groups by name does not reveal private groups to non-members
    Given There is a group with name "Secret Club" with visibility "PRIVATE" owned by "organizer"
    And I login as "outsider" with password "password"
    When I search the groups by name "Secret Club"
    Then The response contains 0 groups
    Given I login as "organizer" with password "password"
    When I search the groups by name "Secret Club"
    Then The response contains 1 group

  # #92 Deleting meetups follows the group administration rules

  Scenario Outline: <user> deletes a meetup of the group
    Given There is a group with name "Gamers" with visibility "PUBLIC" owned by "organizer"
    And There is a membership of "other" in the group with name "Gamers" with role "ADMIN"
    And There is a membership of "participant" in the group with name "Gamers" with role "MEMBER"
    And a group meetup "Game Night" exists in the group "Gamers" created by "<creator>"
    And I login as "<user>" with password "password"
    When I try to delete the meetup "Game Night"
    Then The response code is <code>

    Examples:
      | user        | creator     | code |
      | other       | organizer   | 200  |
      | organizer   | other       | 200  |
      | participant | other       | 403  |

  Scenario: A user cannot delete someone else's meetup
    Given I login as "outsider" with password "password"
    When I try to delete the meetup "Padel Match"
    Then The response code is 403
