Feature: Profile Privacy
  In order to protect my personal data
  As a user
  I want my birth date and gender hidden from others unless I decide to show them

  Background:
    Given There is a registered user with username "user" and password "password" and email "user@sample.app"
    And There is a registered user with username "another" and password "password" and email "another@sample.app"
    And User "user" has birth date "2000-05-20" and gender "MALE"

  Scenario: Other user cannot see personal data by default
    Given I login as "another" with password "password"
    Then The profile of user "user" does not show its personal data

  Scenario: Other user sees personal data when shown
    Given I login as "user" with password "password"
    And I update the profile of user "user" with
      | showPersonalData | true |
    When I login as "another" with password "password"
    Then The profile of user "user" has "birthDate" equal to "2000-05-20"
    And The profile of user "user" has "gender" equal to "MALE"

  Scenario: Owner always sees own personal data
    Given I login as "user" with password "password"
    Then The profile of user "user" has "birthDate" equal to "2000-05-20"
    And The profile of user "user" has "gender" equal to "MALE"
