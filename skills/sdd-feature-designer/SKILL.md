---
name: sdd-feature-designer
description: Design concise, standardized Cucumber Gherkin feature files (.feature) mapped to Spring Data REST endpoints.
---

# SDD Feature Designer Skill

This skill guides AI agents (Aider, OpenCode, Claude Code, Cursor, Copilot) to author concise, high-quality Cucumber BDD `.feature` files for Spring Data REST backends.

## Guidelines
- **Size Constraint**: Keep `.feature` files not too long. Focus on core CRUD & authorization scenarios.
- **Reuse Background Steps**: Always align with standard user registration and authentication setup steps.
- **REST Status Code Mapping**:
  - `201`: Created (POST)
  - `200`: Success (GET / PATCH)
  - `204`: No Content (DELETE)
  - `400`: Bad Request (Validation failure)
  - `403` / `404`: Forbidden or Not Found (Spring Data REST ownership filtering)

## Gherkin Template for Spring Data REST

```gherkin
Feature: Manage <Entity>
    In order to <business goal>
    As a <role>
    I want to be able to create, retrieve, update, and delete <entity>

    Background:
        Given There is a registered user with username "user" and password "password" and email "user@sample.app"
        And There is a registered user with username "another" and password "password" and email "another@sample.app"
        And There is a <entity> with name "Existing Item" owned by "user"

    Scenario: The creator of a <entity> owns it
        Given I login as "user" with password "password"
        When I create a new <entity> with name "New Item"
        Then The response code is 201
        And The new <entity> is owned by "user"

    Scenario: Cannot create a <entity> with invalid attributes
        Given I login as "user" with password "password"
        When I create a new <entity> with name ""
        Then The response code is 400
        And The error message is "must not be blank"

    Scenario: Users can retrieve their own <entity>
        Given I login as "user" with password "password"
        When I retrieve the <entity> with name "Existing Item"
        Then The response code is 200

    Scenario: Users cannot retrieve private <entity> owned by another user
        Given I login as "another" with password "password"
        When I retrieve the <entity> with name "Existing Item"
        Then The response code is 404

    Scenario: Creator can delete their own <entity>
        Given I login as "user" with password "password"
        When I delete the <entity> with name "Existing Item"
        Then The response code is 204
```
