# Project Conventions for Aider & AI Coding Assistants

## Technology Stack
- Java 21, Spring Boot 3.4.1, Spring Data REST, Spring Security, JPA/Hibernate.
- Testing: Cucumber 7.34.4, JUnit 5, MockMvc.

## Architectural Guidelines
1. **Spring Data REST First**: Do NOT implement Spring `@RestController` or Service layers for standard CRUD. Use `@RepositoryRestResource` interfaces.
2. **Entity Pattern**: Domain entities extend `UriEntity<Long>` and use Lombok `@Data` and `@EqualsAndHashCode(callSuper = true)`.
3. **Security Pattern**: Row-level access control MUST be implemented using SpEL queries in `@RepositoryRestResource` interfaces (`r.ownedBy.id = ?#{authentication.name}`).
4. **Lifecycle Hooks**: Use `@RepositoryEventHandler` for setting timestamps (`created`, `modified`) and assigning owners (`SecurityContextHolder`).
5. **BDD Step Definitions**: Do NOT repeat `@SpringBootTest` or Spring context config in step definition classes. Reuse `StepDefs.java`.
6. **Packages structure**: use `domain` for entities, `repository` for repositories and `handler` for event handlers.

## Stage Playbooks (`skills/`)
- `skills/sdd-feature-designer/SKILL.md`
- `skills/sdd-cucumber-steps/SKILL.md`
- `skills/spring-datarest-entity/SKILL.md`
- `skills/spring-datarest-handler/SKILL.md`
- `skills/sdd-bdd-verifier/SKILL.md`
