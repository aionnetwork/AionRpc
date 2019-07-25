package org.aion.api.schema;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;

/**
 * Representation of a JsonSchema reference; i.e. the {@code $ref} keyword; {@see
 * https://json-schema.org/latest/json-schema-core.html#rfc.section.8.3}
 *
 * Example: {@code type.json#/definitions/TAG}
 */
public class JsonSchemaRef {
    private final String refValue;

    /**
     * ctor
     *
     * @param refValue value of the $ref property (always a URI)
     */
    public JsonSchemaRef(String refValue) {
        this.refValue = refValue;
    }

    /** @return the 'file' part of the ref value (before the #) */
    public String getFile() {
        return refValue.split("#")[0];
    }

    /** @return the 'path' part of the ref value (after the #) */
    public String[] getPath() {
        return refValue.split("#")[1].split("/");
    }

    /** @return the 'name' of the path */
    public String getName() {
        String[] parts = refValue.split("#")[1].split("/");
        return parts[parts.length - 1];
    }

    /**
     * Follow the pointer of this reference to get the corresponding JSON node,
     * starting from the given type definitions.
     *
     * @param typeDefinitionsRoot a JsonSchema with {@code definitions} field
     * @return the node that this reference points at
     */
    public JsonNode dereference(JsonNode typeDefinitionsRoot) {
        JsonNode deref = typeDefinitionsRoot;
        JsonNode lastDeref = null;
        // start at 1 because 0 is the root node which we're already in
        for(int ix = 1; ix < getPath().length; ++ix) {
            lastDeref = deref;
            deref = deref.get(getPath()[ix]);
            if(deref == null) {
                throw new SchemaException("Broken reference at: " + lastDeref);
            }
        }

        return deref;
    }

    /** @return the raw value */
    public String getValue() {
        return refValue;
    }

    @Override
    public String toString() {
        return refValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof JsonSchemaRef)) {
            return false;
        }
        JsonSchemaRef that = (JsonSchemaRef) o;
        return Objects.equals(refValue, that.refValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(refValue);
    }
}
