Feature: Change Password
  In order to keep my account secure
  As a user
  I want to be able to change my password

  Background:
    Given There is a registered user with username "user" and password "password" and email "user@sample.app"
    And There is a registered user with username "another" and password "password" and email "another@sample.app"

  Scenario: User changes own password
    Given I login as "user" with password "password"
    When I change the password of user "user" to "newpassword"
    Then The response code is 200
    And I can login with username "user" and password "newpassword"
    And I cannot login with username "user" and password "password"

  Scenario: Cannot change the password of another user
    Given I login as "another" with password "password"
    When I change the password of user "user" to "hackedpassword"
    Then The response code is 403
    And I can login with username "user" and password "password"

  Scenario: Cannot change password when not logged in
    Given I'm not logged in
    When I change the password of user "user" to "newpassword"
    Then The response code is 401
    And I can login with username "user" and password "password"

  Scenario: Cannot change to a password shorter than 8 characters
    Given I login as "user" with password "password"
    When I change the password of user "user" to "short"
    Then The response code is 400
    And The error message is "length must be between 8 and 256"
    And I can login with username "user" and password "password"

  Scenario: Updating other fields keeps the password unchanged
    Given I login as "user" with password "password"
    When I change the email of user "user" to "new@sample.app"
    Then The response code is 200
    And I can login with username "user" and password "password"
