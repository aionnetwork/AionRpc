package org.aion.api.schema;

/** An unexpected structure was encountered while traversing a JsonSchema. */
public class SchemaException extends RuntimeException {
    public SchemaException(String m) {
        super(m);
    }
}
