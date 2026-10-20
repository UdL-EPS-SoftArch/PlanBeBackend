Feature: Notifications
  In order to know what happens with my meetups without checking them all the time
  As a user
  I want to receive notifications, see mine and mark them as read

  Background:
    Given There is a registered user with username "organizer" and password "password" and email "organizer@sample.app"
    And There is a registered user with username "guest" and password "password" and email "guest@sample.app"
    And There is a registered user with username "another" and password "password" and email "another@sample.app"
    And There is a registered user with username "waiter" and password "password" and email "waiter@sample.app"

  # ---------- #41 Notification entity ----------

  Scenario: User lists own notifications
    Given User "guest" has an unread "INVITATION" notification "Mine"
    And User "another" has an unread "INVITATION" notification "Not mine"
    And I login as "guest" with password "password"
    When I list my notifications
    Then The response code is 200
    And The response contains 1 notification
    And The response contains the notification "Mine"

  Scenario: User marks notification as read
    Given User "guest" has an unread "INVITATION" notification "Mine"
    And I login as "guest" with password "password"
    When I mark the notification "Mine" of "guest" as read
    Then The response code is 200
    And The notification "Mine" of "guest" is read
    When I list my unread notifications
    Then The response contains 0 notifications

  Scenario: Cannot see others' notifications
    Given User "guest" has an unread "INVITATION" notification "Private"
    And I login as "another" with password "password"
    When I retrieve the notification "Private" of "guest"
    Then The response code is 404
    When I mark the notification "Private" of "guest" as read
    Then The response code is 404
    And The notification "Private" of "guest" is unread

  Scenario: Admin can inspect a notification but not mark it as read
    Given There is a registered admin with username "admin" and password "password" and email "admin@sample.app"
    And User "guest" has an unread "INVITATION" notification "For the guest"
    And I login as "admin" with password "password"
    When I retrieve the notification "For the guest" of "guest"
    Then The response code is 200
    When I mark the notification "For the guest" of "guest" as read
    Then The response code is 403
    And The notification "For the guest" of "guest" is unread

  Scenario: Users cannot create or alter notifications
    Given User "guest" has an unread "INVITATION" notification "Original"
    And I login as "guest" with password "password"
    When I change the message of the notification "Original" of "guest" to "Forged"
    Then The response code is 403
    And The notification "Original" of "guest" is unread
    When I create a notification for "another" with message "Click this link"
    Then The response code is 403
    And User "another" has no notifications

  # ---------- #42 Invitations ----------

  Scenario: Invite user notifies the invitee
    Given There is a meetup with title "Board Games" organized by "organizer"
    And I login as "organizer" with password "password"
    When I invite "guest" to the meetup "Board Games"
    Then The response code is 201
    And User "guest" has 1 unread "INVITATION" notification
    And The "INVITATION" notification of "guest" points to their invitation to the meetup "Board Games"
    And User "organizer" has no notifications
    Given I login as "guest" with password "password"
    When I list my unread notifications
    Then The response contains 1 "INVITATION" notification

  # ---------- #43 Meetup cancelled or changed ----------

  Scenario: Cancel meetup notifies participants
    Given There is a meetup with title "Hiking" organized by "organizer"
    And "guest" participates in the meetup "Hiking" with status "CONFIRMED"
    And "waiter" participates in the meetup "Hiking" with status "WAITING"
    And "another" participates in the meetup "Hiking" with status "CANCELLED"
    And I login as "organizer" with password "password"
    When I cancel the meetup "Hiking"
    Then The response code is 200
    And User "guest" has 1 unread "MEETUP_CANCELLED" notification
    And User "waiter" has 1 unread "MEETUP_CANCELLED" notification
    And The "MEETUP_CANCELLED" notification of "guest" points to the meetup "Hiking"
    And User "another" has no notifications
    And User "organizer" has no notifications

  Scenario: Change date notifies participants
    Given There is a meetup with title "Hiking" organized by "organizer"
    And "guest" participates in the meetup "Hiking" with status "CONFIRMED"
    And I login as "organizer" with password "password"
    When I change the start date of the meetup "Hiking" to "2030-05-01T09:00:00Z"
    Then The response code is 200
    And User "guest" has 1 unread "MEETUP_UPDATED" notification
    And The "MEETUP_UPDATED" notification of "guest" points to the meetup "Hiking"

  Scenario: Change location notifies participants
    Given There is a meetup with title "Hiking" organized by "organizer"
    And "guest" participates in the meetup "Hiking" with status "CONFIRMED"
    And I login as "organizer" with password "password"
    When I change the city of the meetup "Hiking" to "Lleida"
    Then The response code is 200
    And The meetup "Hiking" is located in "Lleida"
    And User "guest" has 1 unread "MEETUP_UPDATED" notification

  Scenario: Reopening a cancelled meetup notifies participants
    Given There is a meetup with title "Hiking" organized by "organizer"
    And "guest" participates in the meetup "Hiking" with status "CONFIRMED"
    And The meetup "Hiking" is in status "CANCELLED"
    And I login as "organizer" with password "password"
    When I change the status of the meetup "Hiking" to "OPEN"
    Then The response code is 200
    And User "guest" has 1 unread "MEETUP_UPDATED" notification
    And The meetup "Hiking" has status "OPEN" and capacity 10

  Scenario: Other changes or other users do not notify
    Given There is a meetup with title "Hiking" organized by "organizer"
    And "guest" participates in the meetup "Hiking" with status "CONFIRMED"
    And I login as "organizer" with password "password"
    When I change the description of the meetup "Hiking" to "Bring water"
    Then The response code is 200
    Given I login as "another" with password "password"
    When I cancel the meetup "Hiking"
    Then The response code is 403
    And User "guest" has no notifications

  # ---------- #44 Spot freed and group requests ----------

  Scenario: Waiting user promoted gets SPOT_FREED
    Given There is a public meetup with title "Padel" organized by "organizer" and capacity 1
    And "guest" participates in the meetup "Padel" with status "CONFIRMED"
    And "waiter" participates in the meetup "Padel" with status "WAITING"
    And I login as "guest" with password "password"
    When I set my participation in the meetup "Padel" to status "CANCELLED"
    Then The response code is 200
    And The participation of "waiter" in the meetup "Padel" has status "CONFIRMED"
    And User "waiter" has 1 unread "SPOT_FREED" notification
    And The "SPOT_FREED" notification of "waiter" points to the meetup "Padel"
    Given I login as "waiter" with password "password"
    When I list my unread notifications
    Then The response contains 1 "SPOT_FREED" notification

  Scenario: Raising the capacity promotes the waiting list in order
    Given There is a public meetup with title "Padel" organized by "organizer" and capacity 1
    And "guest" participates in the meetup "Padel" with status "CONFIRMED"
    And "waiter" participates in the meetup "Padel" with status "WAITING"
    And "another" participates in the meetup "Padel" with status "WAITING"
    And I login as "organizer" with password "password"
    When I change the capacity of the meetup "Padel" to 2
    Then The response code is 200
    And The participation of "waiter" in the meetup "Padel" has status "CONFIRMED"
    And User "waiter" has 1 unread "SPOT_FREED" notification
    And The participation of "another" in the meetup "Padel" has status "WAITING"
    And User "another" has no notifications
    And The meetup "Padel" has status "FULL" and capacity 2

  Scenario: Admin confirming a waiting user sends SPOT_FREED
    Given There is a registered admin with username "admin" and password "password" and email "admin@sample.app"
    And There is a public meetup with title "Padel" organized by "organizer" and capacity 1
    And "guest" participates in the meetup "Padel" with status "CONFIRMED"
    And "waiter" participates in the meetup "Padel" with status "WAITING"
    And The meetup "Padel" has status "FULL" and capacity 1
    And I login as "admin" with password "password"
    When I set the participation of "waiter" in the meetup "Padel" to status "CONFIRMED"
    Then The response code is 200
    And User "waiter" has 1 unread "SPOT_FREED" notification
    And The participation of "guest" in the meetup "Padel" has status "CONFIRMED"
    And User "guest" has no notifications
    And The meetup "Padel" has status "FULL" and capacity 1

  Scenario: Admin cannot move a confirmed participant to the waiting list
    Given There is a registered admin with username "admin" and password "password" and email "admin@sample.app"
    And There is a public meetup with title "Padel" organized by "organizer" and capacity 1
    And "guest" participates in the meetup "Padel" with status "CONFIRMED"
    And "waiter" participates in the meetup "Padel" with status "WAITING"
    And I login as "admin" with password "password"
    When I set the participation of "guest" in the meetup "Padel" to status "WAITING"
    Then The response code is 403
    And The participation of "guest" in the meetup "Padel" has status "CONFIRMED"
    And The participation of "waiter" in the meetup "Padel" has status "WAITING"
    And User "guest" has no notifications
    And User "waiter" has no notifications

  # Needs Group/Membership (#32, still open). Excluded from the run by the "not @pending" filter in CucumberTest.
  @pending
  Scenario: Join request notifies the group admins
    Given There is a group "Climbers" administered by "organizer"
    And I login as "guest" with password "password"
    When I request to join the group "Climbers"
    Then The response code is 201
    And User "organizer" has 1 unread "GROUP_REQUEST" notification
