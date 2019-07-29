package org.aion.api.schema;

import java.util.HashMap;
import java.util.Map;

/**
 * Container for Json Schema references, keyed by AionRpc type name.  Behaves similar
 * to a {@link Map}.
 */
public class TypeReferences {
    private Map<String, JsonSchemaRef> refsVisited;

    public TypeReferences() {
        this.refsVisited = new HashMap<>();
    }

    public TypeReferences(Map<String, JsonSchemaRef> refs) {
        this.refsVisited = new HashMap<>(refs);
    }

    /**
     * Add a mapping from AionRpc type name (key) to a Json-schema reference (value).
     *
     * The key will be the name of the AionRpc type, i.e. {@link JsonSchemaRef#getTypeName()}.
     * If the given reference uses a name that already exists, the given reference must
     * be the same as the existing reference (results in a no-op).  Otherwise, an
     * exception is thrown.
     *
     * @param ref The reference to add
     * @throws IllegalArgumentException if another reference with the same value for
     * {@link JsonSchemaRef#getTypeName()} but does not {@link JsonSchemaRef#equals(Object)}
     * the given reference.
     */
    public void put(JsonSchemaRef ref) {
        String typeName = ref.getTypeName();
        JsonSchemaRef maybeExistingRef = refsVisited.get(typeName);
        if (maybeExistingRef == null) {
            refsVisited.put(ref.getTypeName(), ref);
        } else if (! ref.equals(maybeExistingRef)) {
            throw new IllegalArgumentException(String.format(
                "Can't use java type name '%s' for ref '%s' because it was already used for '%s'.  "
                    + "Each Json Schema type name must be unique, even if it's in different files.",
                ref.getTypeName(), maybeExistingRef.toString(), ref.toString()));
        }
        // else: nothing to do -- it's already there
    }

    public JsonSchemaRef get(String typeName) {
        return refsVisited.get(typeName);
    }

    public int size() {
        return refsVisited.size();
    }

    public boolean isEmpty() {
        return refsVisited.isEmpty();
    }
}
