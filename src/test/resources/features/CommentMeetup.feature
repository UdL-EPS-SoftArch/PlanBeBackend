Feature: Comment a meetup
  In order to discuss a meetup with the people who can see it
  As a user who can see a meetup
  I want to comment on it and read its comments

  Background:
    Given There is a registered user with username "organizer" and password "password" and email "organizer@sample.app"
    And There is a registered user with username "author" and password "password" and email "author@sample.app"
    And There is a registered user with username "outsider" and password "password" and email "outsider@sample.app"
    And There is a registered user with username "guest" and password "password" and email "guest@sample.app"
    And There is a registered admin with username "admin" and password "password" and email "admin@sample.app"

  Scenario: User comments a public meetup
    Given a meetup "Public Picnic" exists created by "organizer" with visibility "PUBLIC"
    And I login as "author" with password "password"
    When I comment "Looks fun!" on the meetup "Public Picnic"
    Then The response code is 201
    And The comment "Looks fun!" on the meetup "Public Picnic" was written by "author"

  Scenario: Empty comment text is rejected
    Given a meetup "Public Picnic" exists created by "organizer" with visibility "PUBLIC"
    And I login as "author" with password "password"
    When I comment "" on the meetup "Public Picnic"
    Then The response code is 400
    And The meetup "Public Picnic" has 0 comments

  Scenario: Cannot comment a private meetup that is not visible
    Given a meetup "Secret Picnic" exists created by "organizer" with visibility "PRIVATE"
    And I login as "outsider" with password "password"
    When I comment "Can I join?" on the meetup "Secret Picnic"
    Then The response code is 404
    And The meetup "Secret Picnic" has 0 comments

  Scenario: Participant comments a private meetup
    Given a meetup "Secret Picnic" exists created by "organizer" with visibility "PRIVATE"
    And "guest" is a confirmed participant of the meetup "Secret Picnic"
    And I login as "guest" with password "password"
    When I comment "See you there" on the meetup "Secret Picnic"
    Then The response code is 201
    And The comment "See you there" on the meetup "Secret Picnic" was written by "guest"

  Scenario: Outsider cannot read comments of a private meetup
    Given a meetup "Secret Picnic" exists created by "organizer" with visibility "PRIVATE"
    And "guest" is a confirmed participant of the meetup "Secret Picnic"
    And "guest" has already commented "See you there" on the meetup "Secret Picnic"
    And I login as "outsider" with password "password"
    When I list comments
    Then The response contains 0 comments

  Scenario: Participant reads comments of a private meetup
    Given a meetup "Secret Picnic" exists created by "organizer" with visibility "PRIVATE"
    And "guest" is a confirmed participant of the meetup "Secret Picnic"
    And "guest" has already commented "See you there" on the meetup "Secret Picnic"
    And I login as "guest" with password "password"
    When I list comments
    Then The response contains 1 comment
    And The response contains the comment "See you there"

  Scenario: Author deletes their own comment
    Given a meetup "Public Picnic" exists created by "organizer" with visibility "PUBLIC"
    And "author" has already commented "My comment" on the meetup "Public Picnic"
    And I login as "author" with password "password"
    When I delete the comment "My comment" on the meetup "Public Picnic"
    Then The response code is 204
    And The meetup "Public Picnic" has 0 comments

  Scenario: Another user cannot delete a comment
    Given a meetup "Public Picnic" exists created by "organizer" with visibility "PUBLIC"
    And "author" has already commented "My comment" on the meetup "Public Picnic"
    And I login as "outsider" with password "password"
    When I delete the comment "My comment" on the meetup "Public Picnic"
    Then The response code is 403
    And The meetup "Public Picnic" has 1 comment

  Scenario: Admin deletes a comment
    Given a meetup "Public Picnic" exists created by "organizer" with visibility "PUBLIC"
    And "author" has already commented "My comment" on the meetup "Public Picnic"
    And I login as "admin" with password "password"
    When I delete the comment "My comment" on the meetup "Public Picnic"
    Then The response code is 204
    And The meetup "Public Picnic" has 0 comments
