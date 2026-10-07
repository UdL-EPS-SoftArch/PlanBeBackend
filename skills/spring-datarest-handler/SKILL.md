---
name: spring-datarest-handler
description: Generate Spring Data REST RepositoryEventHandlers for lifecycle events and ownership .
---

# Spring Data REST Events Handler

This skill guides AI agents to generate Spring Data REST lifecycle event handlers adhering to the Spring Data REST architecture.

## EventHandler Structure (`@RepositoryEventHandler`)

```java
package cat.udl.eps.softarch.demo.handler;

import cat.udl.eps.softarch.demo.domain.<Entity>;
import cat.udl.eps.softarch.demo.domain.User;
import cat.udl.eps.softarch.demo.repository.<Entity>Repository;
import org.springframework.data.rest.core.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import java.time.ZonedDateTime;

@Component
@RepositoryEventHandler
public class <Entity>EventHandler {
    final <Entity>Repository entityRepository;

    public <Entity>EventHandler(<Entity>Repository entityRepository) {
        this.entityRepository = entityRepository;
    }

    @HandleBeforeCreate
    public void handle<Entity>PreCreate(<Entity> entity) {
        User owner = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        entity.setOwnedBy(owner);
    }

    @HandleBeforeSave
    public void handle<Entity>PreSave(<Entity> entity) {
        User owner = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!owner.getUsername().equals(entity.getOwnedBy().getUsername())) {
            throw new SecurityException("Only owner can modify");
        }
    }
}
```
