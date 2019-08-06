package org.aion.api.schema;

/**
 * A valid JsonSchema construct was used, but in a way that
 * the Aion Rpc framework doesn't allow.
 */
public class SchemaRestrictionException extends RuntimeException {
    public SchemaRestrictionException(String message) {
        super(message);
    }
}
