package org.aion.api.schema;

/**
 * Some data provided to the Aion Rpc layer did not conform to the schema
 * for that input.
 */
public class SchemaValidationException extends Exception {
    public SchemaValidationException(String message) {
        super(message);
    }

    public SchemaValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
