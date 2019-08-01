package org.aion.api.schema;

import java.util.HashMap;
import java.util.Map;

public class TypeRegistry {
    private Map<String, RpcType> index;

    public TypeRegistry() {
        this.index = new HashMap<>();
    }

    /**
     * Add a type.
     *
     * It will be indexed with key name {@link RpcType#getName()}.  If the given
     * type uses a name that's already present in the registry, it must {@link
     * RpcType#equals(Object)} to the existing entry.
     *
     * @param type The reference to add
     * @throws IllegalArgumentException if another reference with the same value for
     * {@link JsonSchemaRef#getTypeName()} but does not {@link JsonSchemaRef#equals(Object)}
     * the given reference.
     */
    public void put(NamedRpcType type) {
        String typeName = type.getName();
        RpcType maybeExistingRef = index.get(typeName);
        if (maybeExistingRef == null) {
            index.put(type.getName(), type);
        } else if (! type.equals(maybeExistingRef)) {
            throw new IllegalArgumentException(String.format(
                "Can't use java type name '%s' for ref '%s' because it was already used for '%s'.  "
                    + "Each Json Schema type name must be unique, even if it's in different files.",
                type.getName(), maybeExistingRef.toString(), type.toString()));
        }
        // else: nothing to do -- it's already there
    }

    public RpcType get(String typeName) {
        return index.get(typeName);
    }

    public int size() {
        return index.size();
    }

    public boolean isEmpty() {
        return index.isEmpty();
    }
}
