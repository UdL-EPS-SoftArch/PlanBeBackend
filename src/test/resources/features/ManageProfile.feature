Feature: Manage Profile
  In order to let other people know who I am
  As a user
  I want to edit my profile, personal data and interests

  Background:
    Given There is a registered user with username "user" and password "password" and email "user@sample.app"
    And There is a registered user with username "another" and password "password" and email "another@sample.app"
    And There is an interest named "Hiking"
    And There is a registered admin with username "admin" and password "password" and email "admin@sample.app"

  Scenario: User edits own profile
    Given I login as "user" with password "password"
    When I update the profile of user "user" with
      | displayName | Diogo                         |
      | bio         | I like mountains              |
      | avatarUrl   | https://example.com/diogo.png |
      | city        | Lleida                        |
    Then The response code is 200
    And The profile of user "user" has "displayName" equal to "Diogo"
    And The profile of user "user" has "city" equal to "Lleida"

  Scenario: User cannot edit another profile
    Given I login as "another" with password "password"
    When I update the profile of user "user" with
      | displayName | Hacked |
    Then The response code is 403

  Scenario: Admin edits any profile
    Given I login as "admin" with password "password"
    When I update the profile of user "user" with
      | displayName | Moderated |
    Then The response code is 200
    And The profile of user "user" has "displayName" equal to "Moderated"

  Scenario: Avatar must be a valid URL
    Given I login as "user" with password "password"
    When I update the profile of user "user" with
      | avatarUrl | not a url |
    Then The response code is 400
    And The error message is "must be a valid URL"

  Scenario: Set birth date and gender
    Given I login as "user" with password "password"
    When I update the profile of user "user" with
      | birthDate | 2000-05-20 |
      | gender    | FEMALE     |
    Then The response code is 200
    And The profile of user "user" has "birthDate" equal to "2000-05-20"
    And The profile of user "user" has "gender" equal to "FEMALE"
    And The profile of user "user" shows the age for birth date "2000-05-20"

  Scenario: Birth date under the minimum age
    Given I login as "user" with password "password"
    When I update the profile of user "user" with
      | birthDate | 2020-01-01 |
    Then The response code is 400
    And The error message is "must be at least 18 years old"

  Scenario: Birth date in the future
    Given I login as "user" with password "password"
    When I update the profile of user "user" with
      | birthDate | 2999-01-01 |
    Then The response code is 400
    And The error message is "must be a past date"

  Scenario: User cannot make themselves admin
    Given I login as "user" with password "password"
    When I update the profile of user "user" with
      | admin | true |
    Then The response code is 200
    And User "user" is not an admin

  Scenario: User adds an interest to own profile
    Given I login as "user" with password "password"
    When I add the interest "Hiking" to user "user"
    Then The response code is 204
    And User "user" has the interest "Hiking"

  Scenario: User removes an interest
    Given User "user" has the interest "Hiking" assigned
    And I login as "user" with password "password"
    When I remove the interest "Hiking" from user "user"
    Then The response code is 204
    And User "user" does not have the interest "Hiking"

  Scenario: User cannot modify another user's interests
    Given I login as "another" with password "password"
    When I add the interest "Hiking" to user "user"
    Then The response code is 403
    And User "user" does not have the interest "Hiking"
