package cat.udl.eps.softarch.demo.domain;

import cat.udl.eps.softarch.demo.domain.entities.CatalogEntityDeserializer;
import cat.udl.eps.softarch.demo.domain.entities.UriEntity;
import cat.udl.eps.softarch.demo.domain.enums.Category;
import tools.jackson.databind.annotation.JsonDeserialize;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;
import org.springframework.data.rest.core.annotation.RestResource;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
public class Interest extends UriEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Length(max = 50)
    @Column(unique = true)
    private String name;

    /** Optional: meetups of this category are recommended to users with this interest (#46). */
    @ManyToOne
    @JoinColumn(name = "category_id")
    @JsonDeserialize(using = CatalogEntityDeserializer.class)
    @RestResource(exported = false)
    private Category category;
}
