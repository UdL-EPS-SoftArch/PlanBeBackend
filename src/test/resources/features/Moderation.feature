Feature: Moderation
  In order to keep the community safe
  As a user I want to block other users and report content,
  and as an admin I want to resolve the reports

  # "user1" is the user of the "I join the meetup" step (JoinMeetup.feature), created with password "password123"
  Background:
    Given There is a registered user with username "blocker" and password "password" and email "blocker@sample.app"
    And There is a registered user with username "user1" and password "password123" and email "user1@sample.app"
    And There is a registered admin with username "admin" and password "password" and email "admin@sample.app"

  # ---------- #48 Block users ----------

  Scenario: User blocks another user
    Given I login as "blocker" with password "password"
    When I block the user "user1"
    Then The response code is 201
    And "blocker" has blocked "user1"

  Scenario: Blocking takes the blocked user out of the blocker's meetups
    Given There is a public meetup with title "Blocker Party" organized by "blocker" and capacity 1
    And "admin" participates in the meetup "Blocker Party" with status "CONFIRMED"
    And "user1" participates in the meetup "Blocker Party" with status "WAITING"
    And There is a meetup with title "Blocker Dinner" organized by "blocker"
    And "user1" has already been invited to the meetup "Blocker Dinner"
    And I login as "blocker" with password "password"
    When I block the user "user1"
    Then The response code is 201
    And The participation of "user1" in the meetup "Blocker Party" has status "CANCELLED"
    And The invitation of "user1" to the meetup "Blocker Dinner" has status "DECLINED"

  Scenario: Blocked user cannot join the blocker's meetup
    Given There is a public meetup with title "Blocker Party" organized by "blocker" and capacity 5
    And "blocker" has already blocked "user1"
    And I am logged in as "user1"
    When I join the meetup "Blocker Party"
    Then The response code is 403
    And "user1" has no participation in the meetup "Blocker Party"

  Scenario: Blocked user cannot invite the blocker
    Given There is a meetup with title "User1 Dinner" organized by "user1"
    And "blocker" has already blocked "user1"
    And I login as "user1" with password "password123"
    When I invite "blocker" to the meetup "User1 Dinner"
    Then The response code is 403
    And There is no invitation of "blocker" to the meetup "User1 Dinner"

  # ---------- #49 Report content ----------

  Scenario: User reports a comment
    Given There is a "FINISHED" meetup "Padel Match" with a confirmed participant "blocker"
    And "blocker" has already reviewed the meetup "Padel Match" with rating 1
    And I login as "user1" with password "password123"
    When I report the review of "blocker" on the meetup "Padel Match" with reason "Insulting comment"
    Then The response code is 201
    And The report "Insulting comment" has status "OPEN"
    And The report "Insulting comment" was made by "user1" about the review of "blocker" on the meetup "Padel Match"

  Scenario: User reports a private meetup they have left, using its link
    Given There is a meetup with title "Blocker Dinner" organized by "blocker"
    And "user1" participates in the meetup "Blocker Dinner" with status "CANCELLED"
    And I login as "user1" with password "password123"
    When I report the meetup "Blocker Dinner" by its link with reason "Fake event"
    Then The response code is 201
    And The report "Fake event" is about the meetup "Blocker Dinner"

  Scenario: Cannot report with an empty reason
    Given I login as "user1" with password "password123"
    When I report the user "blocker" with reason ""
    Then The response code is 400
    And The error message is "must not be blank"
    And "user1" has not reported anything

  Scenario: User only sees their own reports
    Given "user1" has reported the user "blocker" with reason "Spam"
    And "blocker" has reported the user "user1" with reason "Rude messages"
    And I login as "user1" with password "password123"
    When I list the reports
    Then The response contains 1 report
    And The response contains the report "Spam"

  # ---------- #50 Report management by ADMIN ----------

  Scenario: Admin lists all the reports and resolves one
    Given "user1" has reported the user "blocker" with reason "Spam"
    And "blocker" has reported the user "user1" with reason "Rude messages"
    And I login as "admin" with password "password"
    When I list the reports
    Then The response contains 2 reports
    When I mark the report "Spam" as "RESOLVED"
    Then The response code is 200
    And The report "Spam" has status "RESOLVED"

  Scenario: Admin dismisses a report, which then cannot be changed again
    Given "user1" has reported the user "blocker" with reason "Spam"
    And I login as "admin" with password "password"
    When I mark the report "Spam" as "DISMISSED"
    Then The response code is 200
    When I mark the report "Spam" as "RESOLVED"
    Then The response code is 409
    And The report "Spam" has status "DISMISSED"

  Scenario: User cannot change the status of a report
    Given "user1" has reported the user "blocker" with reason "Spam"
    And I login as "user1" with password "password123"
    When I mark the report "Spam" as "RESOLVED"
    Then The response code is 403
    And The report "Spam" has status "OPEN"
