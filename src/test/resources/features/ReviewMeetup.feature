Feature: Review a finished meetup
    In order to share my opinion about a meetup I attended
    As a confirmed participant of a finished meetup
    I want to be able to submit a single review with a rating between 1 and 5

    Background:
        Given There is a registered user with username "reviewer" and password "password" and email "reviewer@sample.app"
        And There is a registered user with username "outsider" and password "password" and email "outsider@sample.app"
        And There is a "FINISHED" meetup "Padel Match" with a confirmed participant "reviewer"

    Scenario: Participant reviews a finished meetup
        Given I login as "reviewer" with password "password"
        When I review the meetup "Padel Match" with rating 5 and text "Great match"
        Then The response code is 201

    Scenario: Cannot review a meetup that is not finished
        Given There is an "OPEN" meetup "Ongoing Match" with a confirmed participant "reviewer"
        And I login as "reviewer" with password "password"
        When I review the meetup "Ongoing Match" with rating 4 and text "Not finished yet"
        Then The response code is 400

    Scenario: Non-participant cannot review a meetup
        Given I login as "outsider" with password "password"
        When I review the meetup "Padel Match" with rating 5 and text "I was not there"
        Then The response code is 403

    Scenario: Cannot review the same meetup twice
        Given "reviewer" has already reviewed the meetup "Padel Match" with rating 5
        And I login as "reviewer" with password "password"
        When I review the meetup "Padel Match" with rating 3 and text "Changed my mind"
        Then The response code is 409

    Scenario: Cannot review with a rating outside the 1 to 5 range
        Given I login as "reviewer" with password "password"
        When I review the meetup "Padel Match" with rating 6 and text "Out of range"
        Then The response code is 400

    Scenario: Organizer with reviews 4 and 5 has an average rating of 4.5
        Given There is a registered user with username "organizer" and password "password" and email "organizer@sample.app"
        And There is a "FINISHED" meetup "First Match" organized by "organizer" with a confirmed participant "reviewer"
        And There is a "FINISHED" meetup "Second Match" organized by "organizer" with a confirmed participant "reviewer"
        And "reviewer" has already reviewed the meetup "First Match" with rating 4
        And "reviewer" has already reviewed the meetup "Second Match" with rating 5
        When I request the average rating of organizer "organizer"
        Then The response code is 200
        And The average rating is 4.5

    Scenario: Organizer without reviews has no average rating
        Given There is a registered user with username "organizer" and password "password" and email "organizer@sample.app"
        When I request the average rating of organizer "organizer"
        Then The response code is 200
        And The average rating is null
