package cat.udl.eps.softarch.demo.domain;

import cat.udl.eps.softarch.demo.domain.Entity.UriEntity;
import cat.udl.eps.softarch.demo.domain.enums.Category;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;

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
    @Enumerated(EnumType.STRING)
    private Category category;
}
