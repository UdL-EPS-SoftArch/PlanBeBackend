Feature: Meetup Visibility
  As a user
  I want to control the visibility of my meetups
  So that I can keep some meetups private

  Scenario: Anonymous lists public meetups
    Given I'm not logged in
    And a meetup "Public Meetup" exists with visibility "PUBLIC"
    And a meetup "Private Meetup" exists with visibility "PRIVATE"
    When I list meetups
    Then I should see "Public Meetup"
    And I should not see "Private Meetup"

  Scenario: Other user cannot retrieve private meetup
    Given I'm logged in as "user"
    And a meetup "Secret Meetup" exists created by "organizer" with visibility "PRIVATE"
    When I try to retrieve the meetup "Secret Meetup"
    Then the response code is 403

  Scenario: Organizer retrieves own private meetup
    Given I'm logged in as "organizer"
    And a meetup "My Secret Meetup" exists with visibility "PRIVATE"
    When I try to retrieve the meetup "My Secret Meetup"
    Then the response code is 200
