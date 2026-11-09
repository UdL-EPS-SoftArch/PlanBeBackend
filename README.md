# PlanBe Backend

Backend de **PlanBe**, una plataforma para crear, descubrir y unirse a quedadas entre personas y grupos.

[![Open Issues](https://img.shields.io/github/issues-raw/UdL-EPS-SoftArch/PlanBeBackend?logo=github)](https://github.com/UdL-EPS-SoftArch/PlanBeBackend/issues)
[![CI/CD](https://github.com/UdL-EPS-SoftArch/PlanBeBackend/actions/workflows/maven.yml/badge.svg)](https://github.com/UdL-EPS-SoftArch/PlanBeBackend/actions/workflows/maven.yml)

## Visión
PlanBe propone un backend REST (HAL) para gestionar planes sociales con reglas de visibilidad, participación, moderación y recomendaciones personalizadas, siguiendo un enfoque SDD (Spec-Driven Development) con Cucumber.

## Actores del sistema
- **Anónimo**: registro y consulta de quedadas públicas.
- **Usuario**: gestiona perfil, crea quedadas, participa, invita, comenta y valora.
- **Miembro/Admin de grupo**: administra grupos y su participación en quedadas.
- **Admin de plataforma (`ROLE_ADMIN`)**: moderación y gestión avanzada (intereses, reportes, bloqueos).

## Funcionalidades principales
- **Usuarios y perfiles**: registro/login, perfil, privacidad de datos personales, intereses.
- **Quedadas**: CRUD, visibilidad (pública, privada o de grupo), estado, capacidad, categoría, etiquetas y validaciones.
- **Participación e invitaciones**: unirse, invitaciones, lista de espera y transiciones de estado.
- **Grupos**: creación/gestión de grupos, membresías con roles y quedadas de grupo gestionadas por sus administradores.
- **Comentarios**: comentarios sobre quedadas con control de acceso.
- **Valoraciones**: reseñas de participantes sobre quedadas finalizadas.
- **Notificaciones**: bandeja de notificaciones y marcado como leídas.
- **Recomendaciones**: quedadas públicas y abiertas de la ciudad del usuario que coinciden con sus intereses (por etiqueta o categoría).
- **Moderación**: bloqueos entre usuarios y reportes con flujo de revisión.
- **Quedadas recurrentes**: recurrencia semanal, quincenal o mensual con fecha de fin opcional; al finalizar una ocurrencia se genera la siguiente.
- **Chat de quedada**: mensajes entre participantes confirmados, vía REST (`/chatMessages`) y en tiempo real por WebSocket/STOMP.

## Arquitectura y stack
- **Java 21**
- **Spring Boot**
- **Spring Data JPA**
- **Spring Data REST + HAL Explorer**
- **Spring WebSocket (STOMP)** para el chat en tiempo real
- **Spring Security (Basic Auth) + BCrypt**
- **Bean Validation**
- **H2 / PostgreSQL**
- **Cucumber + JUnit Platform**

## Restricciones arquitectónicas del proyecto
- Exposición REST mediante repositorios `@RepositoryRestResource`; las consultas internas llevan `@RestResource(exported = false)`.
- Reglas de ciclo de vida y seguridad de dominio en handlers `@RepositoryEventHandler`.
- Autorización a nivel de fila con SpEL en consultas `@Query`.
- Endpoint auxiliar de identidad autenticada: `GET /identity`.

## Modelo de dominio (resumen)
El modelo completo, con campos y enumerados, está en [`EntitiesModel.puml`](EntitiesModel.puml). Todas las entidades extienden `UriEntity` (`domain/entities`) y los enumerados están en `domain/enums`.

```mermaid
classDiagram
    class User
    class Interest
    class Meetup
    class Location
    class Participation
    class Invitation
    class Group
    class Membership
    class Comment
    class Review
    class Notification
    class Block
    class Report
    class ChatMessage

    User "*" -- "*" Interest : interests
    Meetup "*" -- "*" Interest : tags
    Meetup *-- Location
    Meetup "*" --> "1" User : organizer
    Meetup "*" --> "0..1" Group : group
    Meetup "0..1" --> "0..1" Meetup : previousOccurrence

    Participation "*" --> "1" Meetup
    Participation "*" --> "1" User : participant
    Invitation "*" --> "1" Meetup
    Invitation "*" --> "1" User : invitee
    Invitation "*" --> "1" User : invitedBy

    Group "*" --> "1" User : ownedBy
    Membership "*" --> "1" Group
    Membership "*" --> "1" User : member

    Comment "*" --> "1" Meetup
    Comment "*" --> "1" User : author
    Review "*" --> "1" Meetup
    Review "*" --> "1" User : reviewer
    ChatMessage "*" --> "1" Meetup
    ChatMessage "*" --> "1" User : sender

    Notification "*" --> "1" User : recipient
    Block "*" --> "1" User : blocker
    Block "*" --> "1" User : blocked
    Report "*" --> "1" User : reporter
```

## Endpoints principales
### Recursos
`/users`, `/interests`, `/meetups`, `/participations`, `/invitations`, `/groups`, `/memberships`, `/comments`, `/reviews`, `/chatMessages`, `/notifications`, `/blocks`, `/reports` y `/identity`.

Chat en tiempo real: endpoint STOMP `/ws`; se envía a `/app/meetups/{id}/chat` y se recibe en `/topic/meetups/{id}/chat`.

### Búsquedas disponibles (Spring Data REST)
- `/users/search/findByIdContaining`
- `/interests/search/findByName`
- `/interests/search/findByNameContainingIgnoreCase`
- `/meetups/search/findVisibleMeetups`
- `/meetups/search/findByCategoryAndVisibility`
- `/meetups/search/findByCityAndVisibility`
- `/meetups/search/findByStartDateAfterAndVisibility`
- `/meetups/search/findByOrganizerAndVisibility`
- `/meetups/search/recommended`
- `/participations/search/findMyParticipations`
- `/participations/search/findByParticipantIdAndMeetupId`
- `/groups/search/findByOwnedBy`
- `/groups/search/findByName`
- `/memberships/search/findByGroupId`
- `/notifications/search/findUnread`
- `/reports/search/findByStatus`
- `/reviews/search/findByReviewerIdAndMeetupId`
- `/reviews/search/findByMeetupId`
- `/reviews/search/findAverageRatingByOrganizer`
- `/chatMessages/search/findByMeetupId`

Las búsquedas aplican el mismo filtro de filas que el listado de su recurso.

## Reglas de acceso
- **Sin autenticación**: registro (`POST /users`), lectura de intereses, y listados/búsquedas de quedadas, grupos y valoraciones, filtrados a lo que es público.
- **Usuario autenticado**: cualquier escritura (`POST`/`PUT`/`PATCH`/`DELETE`), los recursos individuales (`/x/{id}`) y sus asociaciones (`/x/{id}/y`), usuarios, participaciones, invitaciones, notificaciones, bloqueos, reportes y recomendaciones.
- **Filtro por filas**: cada repositorio limita lo que ve el usuario (p. ej. una quedada privada solo la ven su organizador, sus invitados y participantes, los miembros activos de su grupo y un ADMIN; una participación, su participante, el organizador, los gestores del grupo de la quedada y un ADMIN; las valoraciones siguen la visibilidad de su quedada).
- **Solo `ROLE_ADMIN`**: crear, modificar y borrar intereses; resolver, modificar y borrar reportes.

Referencia funcional completa y objetivo de endpoints: [`docs/PLAN.md`](docs/PLAN.md).

## Requisitos y ejecución con Maven
### Requisitos
- JDK **21**
- Maven 3.9+

### Comandos
- Ejecutar aplicación (perfil por defecto):
  - `mvn spring-boot:run`
- Ejecutar aplicación con PostgreSQL (`DATABASE_URL` + perfil `postgres`):
  - `mvn spring-boot:run -Dspring-boot.run.profiles=postgres`
- Ejecutar tests:
  - `mvn test`
- Ejecutar suite BDD Cucumber:
  - `mvn test -Dtest=CucumberTest`

## Roadmap SDD (basado en `docs/PLAN.md`)
| Iteración | Objetivo | Estado en repositorio |
|---|---|---|
| 0 | Registro/login base | Implementado |
| 1 | Perfil e intereses | Implementado |
| 2 | Gestión de quedadas | Implementado |
| 3 | Participación y aforo/lista de espera | Implementado |
| 4 | Invitaciones | Implementado |
| 5 | Grupos y membresías | Implementado |
| 6 | Comentarios | Implementado |
| 7 | Valoraciones | Implementado |
| 8 | Notificaciones | Implementado |
| 9 | Recomendaciones | Implementado |
| 10 | Moderación (bloqueos/reportes) | Implementado |
| 11 | Recurrencia de quedadas | Implementado |
| 12 | Chat de quedada (REST y WebSocket) | Implementado |
| 13 | Correcciones del feedback (seguridad, tests, documentación) | En curso |

## Documentación relacionada
- Plan funcional y roadmap: [`docs/PLAN.md`](docs/PLAN.md)
- Modelo de entidades (PlantUML): [`EntitiesModel.puml`](EntitiesModel.puml)
- Flujo de desarrollo asistido por IA: [`AI_DEVELOPMENT_GUIDE.md`](AI_DEVELOPMENT_GUIDE.md)
- Convenciones del repositorio: [`CONVENTIONS.md`](CONVENTIONS.md)
- Guía de agentes: [`AGENTS.md`](AGENTS.md)
- Ayuda adicional: [`HELP.md`](HELP.md)
- Licencia: [`LICENSE`](LICENSE)
