package cat.udl.eps.softarch.demo.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.atteo.evo.inflector.English;
import org.springframework.data.domain.Persistable;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * Base class for the volatile domain concepts that used to be Java enums and are now catalog tables.
 * The identifier is the former enum constant name, so existing code and JSON payloads can keep using
 * values such as "OPEN" or "CONFIRMED" while the database stores a strict foreign key.
 *
 * The identifier is assigned (not generated) and there is no version column: instances such as the
 * former enum constants are detached references that Hibernate can use as a foreign key directly.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class CatalogEntity implements Persistable<String> {

    @Id
    @Column(length = 64)
    private String id;

    protected CatalogEntity() {
    }

    protected CatalogEntity(String id) {
        this.id = id;
    }

    /** Former enum-style accessor: the catalog value name. */
    @JsonValue
    public String name() {
        return id;
    }

    public String getUri() {
        return "/" + English.plural(StringUtils.uncapitalize(this.getClass().getSimpleName())) + "/" + getId();
    }

    @Override
    @JsonIgnore
    public boolean isNew() {
        return id == null;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        return Objects.equals(id, ((CatalogEntity) other).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), id);
    }

    @Override
    public String toString() {
        return id;
    }
}
