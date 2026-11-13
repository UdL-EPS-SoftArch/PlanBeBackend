package cat.udl.eps.softarch.demo.domain.enums;

import cat.udl.eps.softarch.demo.domain.entities.CatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Interest and group categories are business-driven and change at runtime, so they live in a catalog table
 * (managed through /categories) instead of a Java enum.
 */
@Entity
@Table(name = "Category")
@Getter
@Setter
public class Category extends CatalogEntity {

    public static final Category TECHNOLOGY = new Category("TECHNOLOGY");
    public static final Category SPORTS = new Category("SPORTS");
    public static final Category ARTS = new Category("ARTS");
    public static final Category CULTURE = new Category("CULTURE");
    public static final Category EDUCATION = new Category("EDUCATION");
    public static final Category HEALTH = new Category("HEALTH");
    public static final Category OTHER = new Category("OTHER");

    private String description;
    private String color;

    public Category() {
    }

    public Category(String id) {
        super(id);
    }

    public static Category valueOf(String name) {
        return new Category(name);
    }

    public static List<Category> values() {
        return List.of(TECHNOLOGY, SPORTS, ARTS, CULTURE, EDUCATION, HEALTH, OTHER);
    }
}
