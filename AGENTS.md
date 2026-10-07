# Agent Guidelines for Spring Boot Template

This repository is a **Spring Data REST + Cucumber BDD** backend template designed for Spec-Driven Development (SDD) with AI agents.

## Core Architectural Rules
- **No Manual REST Controllers**: Expose JPA entities via Spring Data REST interfaces (`@RepositoryRestResource`).
- **Entity Identity**: Domain entities extend `UriEntity<Long>` with Lombok `@Data` and `@EqualsAndHashCode(callSuper = true)`.
- **Row-Level Authorization**: Enforce ownership SpEL checks in `@Query` annotations (`r.ownedBy.id = ?#{authentication.name} OR ?#{hasRole('ADMIN')} = true`).
- **Lifecycle Events**: Use `@RepositoryEventHandler` (`@HandleBeforeCreate`, `@HandleBeforeSave`) for ownership and security checks.

## SDD Skills Directory
Refer to `skills/` for specific stage playbooks:
- `skills/sdd-feature-designer/SKILL.md`: Draft Cucumber `.feature` specs, keep not too long.
- `skills/sdd-cucumber-steps/SKILL.md`: Implement Cucumber step definitions extending `StepDefs.java`.
- `skills/spring-datarest-entity/SKILL.md`: Generate domain entities and their `@RepositoryRestResource` interfaces with SpEL queries.
- `skills/spring-datarest-handler/SKILL.md`: Generate `@RepositoryEventHandler` for entities lifecycle events management.
- `skills/sdd-bdd-verifier/SKILL.md`: Run `mvn test -Dtest=CucumberTest` and analyze failures.

Detailed AI workflows are documented in `AI_DEVELOPMENT_GUIDE.md`.
