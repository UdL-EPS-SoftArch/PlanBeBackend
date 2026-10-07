---
name: spring-datarest-entity
description: Create JPA domain entities extending UriEntity and REST repositories (@RepositoryRestResource) with SpEL security queries.
---

# Spring Data REST Entity & Repository Skill

Guide AI agents to generate JPA domain entities and REST Repositories adhering to Spring Data REST. Just model one size of relationships, the @ManyToOne or for @ManyToMany prefer the smallest set or that owning the relationship. Use findBy... in the corresponding Repository for the other side of the relationship. Thus, avoid modelling both sides and using "mappedBy". Also avoid @JoinTable, @JoinColumn and related annotations, use the default naming conventions. Use @JsonIdentityReference(alwaysAsId = true) to avoid circular references in JSON serialization.

## Entity Structure

```java
package cat.udl.eps.softarch.demo.domain;

import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.Instant;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Data
@EqualsAndHashCode(callSuper = true)
public class <Entity> extends UriEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    @Length(max = 500)
    private String description;

    @CreatedDate
    @DateTimeFormat
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Instant created;

    @LastModifiedDate
    @DateTimeFormat
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Instant modified;

    @ManyToOne
    @JsonIdentityReference(alwaysAsId = true)
    private User ownedBy;
}
```

## Repository Structure

```java
package cat.udl.eps.softarch.demo.repository;

import cat.udl.eps.softarch.demo.domain.<Entity>;
import cat.udl.eps.softarch.demo.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource
public interface <Entity>Repository extends CrudRepository<<Entity>, Long>, PagingAndSortingRepository<<Entity>, Long> {

    String OWNERSHIP_CLAUSE =
        "e.ownedBy.id = ?#{authentication.name} " +
        "OR ?#{hasRole('ADMIN')} = true";

    @Override
    @Query("SELECT e FROM <Entity> e WHERE " + OWNERSHIP_CLAUSE)
    Page<<Entity>> findAll(Pageable pageable);

    @Override
    @Query("SELECT e FROM <Entity> e WHERE e.id = :id AND (" + OWNERSHIP_CLAUSE + ")")
    Optional<<Entity>> findById(@Param("id") Long id);

    @Query("SELECT e FROM <Entity> e WHERE e.ownedBy = :user AND (" + OWNERSHIP_CLAUSE + ")")
    Page<<Entity>> findByOwnedBy(@Param("user") User owner, Pageable pageable);

    List<<Entity>> findByName(String name);
    List<<Entity>> findByOwnedBy(User owner);
}
```
