Feature: Recommend Meetups
  In order to discover meetups I may like
  As a user
  I want to get the public meetups in my city that match my interests

  Background:
    Given There is a registered user with username "user" and password "password" and email "user@sample.app"
    And There is a registered user with username "organizer" and password "password" and email "organizer@sample.app"
    And There is an interest named "SPORT"
    And There is an interest named "MUSIC"
    And User "user" has the interest "SPORT" assigned
    And User "user" lives in "Lleida"

  Scenario: User gets the open public meetups in their city that match their interests
    Given There are the following meetups organized by "organizer":
      | title       | city   | tags  | startsInDays |
      | Padel Match | Lleida | SPORT | 3            |
      | Morning Run | lleida | SPORT | 1            |
      | Jazz Night  | Lleida | MUSIC | 2            |
    And I login as "user" with password "password"
    When I request my recommended meetups
    Then The response code is 200
    And The recommended meetups are "Morning Run, Padel Match"

  Scenario: Meetups in other cities are not recommended
    Given There are the following meetups organized by "organizer":
      | title          | city      | tags  | startsInDays |
      | Padel Match    | Lleida    | SPORT | 3            |
      | Football Match | Barcelona | SPORT | 3            |
    And I login as "user" with password "password"
    When I request my recommended meetups
    Then The recommended meetups are "Padel Match"

  Scenario: Full, cancelled, private or already started meetups are not recommended
    Given There are the following meetups organized by "organizer":
      | title         | city   | tags  | startsInDays | status    | visibility |
      | Padel Match   | Lleida | SPORT | 3            | OPEN      | PUBLIC     |
      | Full Match    | Lleida | SPORT | 3            | FULL      | PUBLIC     |
      | Cancelled Run | Lleida | SPORT | 3            | CANCELLED | PUBLIC     |
      | Secret Match  | Lleida | SPORT | 3            | OPEN      | PRIVATE    |
      | Past Match    | Lleida | SPORT | -1           | OPEN      | PUBLIC     |
    And I login as "user" with password "password"
    When I request my recommended meetups
    Then The recommended meetups are "Padel Match"

  Scenario: Meetups whose category matches the category of one of the user's interests are recommended
    Given The interest "SPORT" belongs to the category "SPORTS"
    And There are the following meetups organized by "organizer":
      | title       | city   | tags  | category | startsInDays |
      | Trail Run   | Lleida |       | SPORTS   | 2            |
      | Padel Match | Lleida | MUSIC | SPORTS   | 3            |
      | Art Expo    | Lleida |       | ARTS     | 1            |
      | Concert     | Lleida | MUSIC |          | 1            |
    And I login as "user" with password "password"
    When I request my recommended meetups
    Then The recommended meetups are "Trail Run, Padel Match"

  Scenario: Category alone does not recommend when the user's interests have no category
    Given There are the following meetups organized by "organizer":
      | title     | city   | tags | category | startsInDays |
      | Trail Run | Lleida |      | SPORTS   | 2            |
    And I login as "user" with password "password"
    When I request my recommended meetups
    Then The response code is 200
    And There are no recommended meetups

  Scenario: Own meetups and meetups already joined are not recommended
    Given There are the following meetups organized by "organizer":
      | title       | city   | tags  | startsInDays |
      | Padel Match | Lleida | SPORT | 3            |
      | Joined Run  | Lleida | SPORT | 3            |
    And There are the following meetups organized by "user":
      | title    | city   | tags  | startsInDays |
      | My Match | Lleida | SPORT | 3            |
    And "user" is a confirmed participant of the meetup "Joined Run"
    And I login as "user" with password "password"
    When I request my recommended meetups
    Then The recommended meetups are "Padel Match"

  Scenario: A meetup tagged by its organizer is recommended
    Given I login as "organizer" with password "password"
    And I create a meetup "Climbing Day" in "Lleida" tagged with "SPORT"
    And The response code is 201
    And I login as "user" with password "password"
    When I request my recommended meetups
    Then The recommended meetups are "Climbing Day"

  Scenario: User without a city gets no recommendations
    Given User "user" has no city
    And There are the following meetups organized by "organizer":
      | title       | city   | tags  | startsInDays |
      | Padel Match | Lleida | SPORT | 3            |
    And I login as "user" with password "password"
    When I request my recommended meetups
    Then The response code is 200
    And There are no recommended meetups

  Scenario: Cannot get recommendations when not logged in
    Given I'm not logged in
    When I request my recommended meetups
    Then The response code is 401
