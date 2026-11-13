package cat.udl.eps.softarch.demo.domain.entities;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;

/**
 * Reads a catalog reference from its former enum name (e.g. {@code "OPEN"}) using the catalog's
 * {@code valueOf(String)} factory, so clients keep sending plain strings instead of catalog URIs.
 */
public class CatalogEntityDeserializer extends ValueDeserializer<CatalogEntity> {

    private final Class<? extends CatalogEntity> catalogType;

    public CatalogEntityDeserializer() {
        this(CatalogEntity.class);
    }

    private CatalogEntityDeserializer(Class<? extends CatalogEntity> catalogType) {
        this.catalogType = catalogType;
    }

    @Override
    public ValueDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
        JavaType contextualType = ctxt.getContextualType();
        if (contextualType == null && property != null) {
            contextualType = property.getType();
        }
        if (contextualType == null) {
            return this;
        }
        Class<?> rawClass = contextualType.getRawClass();
        if (!CatalogEntity.class.isAssignableFrom(rawClass) || rawClass == CatalogEntity.class) {
            return this;
        }
        return new CatalogEntityDeserializer(rawClass.asSubclass(CatalogEntity.class));
    }

    @Override
    public CatalogEntity deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        String name = p.getValueAsString();
        if (name == null) {
            return null;
        }
        try {
            return (CatalogEntity) catalogType.getMethod("valueOf", String.class).invoke(null, name);
        } catch (ReflectiveOperationException cause) {
            throw new IllegalArgumentException(
                "Invalid catalog value '%s' for %s".formatted(name, catalogType.getSimpleName()), cause);
        }
    }
}
