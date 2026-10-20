Feature: Search Meetups
  As a user
  I want to search for meetups by various criteria
  So that I can find events that interest me

  Scenario: Search by category
    Given the following meetups exist:
      | title            | category    | visibility | city   |
      | Tech Talk        | TECHNOLOGY | PUBLIC     | Barcelona |
      | Art Workshop     | ARTS        | PUBLIC     | Barcelona |
      | Private Tech     | TECHNOLOGY | PRIVATE    | Madrid |
    And I'm not logged in
    When I search for meetups by category "TECHNOLOGY"
    Then I should see "Tech Talk"
    And I should not see "Art Workshop"
    And I should not see "Private Tech"

  Scenario: Search by city
    Given the following meetups exist:
      | title            | category    | visibility | city       |
      | BCN Meetup       | TECHNOLOGY | PUBLIC     | Barcelona  |
      | MAD Meetup       | TECHNOLOGY | PUBLIC     | Madrid     |
      | Private BCN      | TECHNOLOGY | PRIVATE    | Barcelona  |
    And I'm not logged in
    When I search for meetups by city "Barcelona"
    Then I should see "BCN Meetup"
    And I should not see "MAD Meetup"
    And I should not see "Private BCN"

  Scenario: Search upcoming after date
  	Given the following meetups exist:
      | title          | startDate            | visibility |
      | Past Meetup    | 2026-10-14T00:00:00Z | PUBLIC     |
      | Future Meetup  | 2099-01-01T00:00:00Z | PUBLIC     |
      | Private Future | 2099-01-01T00:00:00Z | PRIVATE    |
    And I'm not logged in
    When I search for meetups after "2026-10-14T01:00:00Z"
    Then I should see "Future Meetup"
    And I should not see "Past Meetup"
    And I should not see "Private Future"


  Scenario: Search by organizer
    Given the following users exist:
      | username | password |
      | organizer | password |
    And the following meetups exist:
      | title            | organizer  | visibility |
      | Org Meetup 1     | organizer  | PUBLIC     |
      | Org Meetup 2     | organizer  | PRIVATE    |
      | Other Meetup     | other       | PUBLIC     |
    And I'm not logged in
    When I search for meetups by organizer "organizer"
    Then I should see "Org Meetup 1"
    And I should not see "Other Meetup"
    And I should not see "Org Meetup 2"
