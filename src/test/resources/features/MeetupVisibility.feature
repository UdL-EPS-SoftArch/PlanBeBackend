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

  Scenario: Invited user retrieves private meetup
    Given There is a registered user with username "guest" and password "password" and email "guest@sample.app"
    And a meetup "Invite Only" exists created by "organizer" with visibility "PRIVATE"
    And "guest" has already been invited to the meetup "Invite Only"
    And I'm logged in as "guest"
    When I try to retrieve the meetup "Invite Only"
    Then the response code is 200

  Scenario: Invited user sees private meetup when listing
    Given There is a registered user with username "guest" and password "password" and email "guest@sample.app"
    And a meetup "Invite Only" exists created by "organizer" with visibility "PRIVATE"
    And "guest" has already been invited to the meetup "Invite Only"
    And I'm logged in as "guest"
    When I list the meetups visible to me
    Then I should see "Invite Only"

  Scenario: User who declined the invitation cannot retrieve private meetup
    Given There is a registered user with username "guest" and password "password" and email "guest@sample.app"
    And a meetup "Invite Only" exists created by "organizer" with visibility "PRIVATE"
    And "guest" has already been invited to the meetup "Invite Only"
    And The invitation of "guest" to the meetup "Invite Only" was already answered with "DECLINED"
    And I'm logged in as "guest"
    When I try to retrieve the meetup "Invite Only"
    Then the response code is 403

  Scenario: Participant retrieves private meetup
    Given There is a registered user with username "guest" and password "password" and email "guest@sample.app"
    And a meetup "Invite Only" exists created by "organizer" with visibility "PRIVATE"
    And "guest" is a confirmed participant of the meetup "Invite Only"
    And I'm logged in as "guest"
    When I try to retrieve the meetup "Invite Only"
    Then the response code is 200

  Scenario: Non-invited user does not see private meetup when listing
    Given There is a registered user with username "another" and password "password" and email "another@sample.app"
    And a meetup "Invite Only" exists created by "organizer" with visibility "PRIVATE"
    And I'm logged in as "another"
    When I list the meetups visible to me
    Then I should not see "Invite Only"
