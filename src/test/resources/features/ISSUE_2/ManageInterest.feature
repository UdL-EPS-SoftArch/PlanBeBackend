Feature: Manage Interest
  In order to describe users and meetups with common topics
  As an admin
  I want to manage the catalogue of interests

  Background:
    Given There is a registered user with username "user" and password "password" and email "user@sample.app"
    And There is a registered admin with username "admin" and password "password" and email "admin@sample.app"

  Scenario: Admin creates an interest
    Given I login as "admin" with password "password"
    When I create an interest named "Hiking"
    Then The response code is 201
    And There is an interest named "Hiking" in the catalogue

  Scenario: User cannot create an interest
    Given I login as "user" with password "password"
    When I create an interest named "Hiking"
    Then The response code is 403
    And There is no interest named "Hiking" in the catalogue

  Scenario: Cannot create a duplicated interest
    Given There is an interest named "Hiking"
    And I login as "admin" with password "password"
    When I create an interest named "Hiking"
    Then The response code is 409

  Scenario: Anyone lists the interests
    Given There is an interest named "Hiking"
    And I'm not logged in
    When I list the interests
    Then The response code is 200
    And The interest list contains "Hiking"
