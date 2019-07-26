package org.aion.api.schema;

import java.util.HashMap;
import java.util.Map;

public class JsonReferences {
    private Map<String, JsonSchemaRef> refsVisited;

    public JsonReferences() {
        this.refsVisited = new HashMap<>();
    }

    public JsonReferences(Map<String, JsonSchemaRef> refs) {
        this.refsVisited = new HashMap<>(refs);
    }

    /**
     * Add a mapping from AionRpc type name (key) to a Json-schema reference (value).
     *
     * If the given key already exists and the corresponding existing value
     * is equal to the given value, there is no effect is false is returned.
     *
     * If the given key does not exist, the mapping is added and true is
     * returned.
     *
     * If the given key already exists but has a different corresponding
     * value than the given one, {@link SchemaException} is thrown.
     *
     * @param typeName AionApc type name
     * @param ref JSON pointer to a JsonSchema that defines that type
     * @return true if new reference added, false is it already exists (no-op)
     * @throws IllegalArgumentException if the given type name already exists and
     *         has a different reference than the given one.
     */
    public boolean put(String typeName, JsonSchemaRef ref) {
        JsonSchemaRef maybeExistingRef = refsVisited.get(ref.getName());
        if (maybeExistingRef == null) {
            refsVisited.put(ref.getName(), ref);
            return true;
        } else if (! ref.equals(maybeExistingRef)) {
            throw new IllegalArgumentException(String.format(
                "Can't use java type name '%s' for ref '%s' because it was already used for '%s'.  "
                    + "Each Json Schema type name must be unique, even if it's in different files.",
                ref.getName(), maybeExistingRef.toString(), ref.toString()));
        }

        // else: nothing to do -- it's already there
        return false;
    }

    public JsonSchemaRef get(String typeName) {
        return refsVisited.get(typeName);
    }

    public int size() {
        return refsVisited.size();
    }
}
