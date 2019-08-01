package org.aion.api.schema;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;

/**
 * JsonSchema reference for an AionRpc type.  Not intended for general-purpose
 * JsonSchema $ref or JsonPointer representation, but specific to how AionRpc
 * uses it.
 *
 * Example value: {@code type.json#/definitions/TAG}
 *
 * As per JsonSchema spec, the value is a URI.
 *   - The part before the # is a file, given by {@link #getFile()}.
 *   - The part after the # is called the fragment, given by {@link #getFragmentParts()}.
 *   - The last element in the fragment is assumed to the be the AionRpc type
 *     name, given by {@link #getTypeName()}.
 *
 * @implNote Details on JsonSchema $ref keyword: {@see
 * https://json-schema.org/latest/json-schema-core.html#rfc.section.8.3}.
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

    /** @return the 'fragment' part of the ref value (after the #) */
    public String[] getFragmentParts() {
        if(refValue.contains("#")) {
            return refValue.split("#")[1]
                .replaceFirst("/", "")
                .split("/");
        } else {
            return refValue.replaceFirst("/", "")
                .split("/");
        }
    }

    public String getFragment() {
        if(refValue.contains("#")) {
            return refValue.split("#")[1];
        } else {
            return refValue;
        }
    }

    /**
     * @return The AionRpc type name, which is defined to be the last element
     * in {@link #getFragmentParts()}.
     */
    public String getTypeName() {
        String[] path = getFragmentParts();
        String[] parts = path[path.length - 1].split("/");
        return parts[parts.length - 1];
    }

    /**
     * Follow the fragment of this reference to get the corresponding JSON node,
     * starting from the given type definitions.  File lookups are not supported
     * right now -- the {@link #getFile()} portion of this reference is not used;
     * it is assumed that the given typeDefinitionsRoot is the JSON Root that
     * that file contains.
     *
     * @param typeDefinitionsRoot a JsonSchema with {@code definitions} field
     * @return the node that this reference points at
     */
    public JsonNode getDefinition(JsonNode typeDefinitionsRoot) {
        JsonNode deref = typeDefinitionsRoot;
        JsonNode lastDeref = null;

        for(int ix = 0; ix < getFragmentParts().length; ++ix) {
            lastDeref = deref;
            deref = deref.get(getFragmentParts()[ix]);
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
