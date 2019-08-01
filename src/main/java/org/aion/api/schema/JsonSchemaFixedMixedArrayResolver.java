package org.aion.api.schema;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Similar to {@link JsonSchemaTypeResolver}, but specialized for a specific purpose:
 * resolving the types for an array whose schema defines it as fixed-size with different
 * types each each element.  The normal {@link JsonSchemaTypeResolver} does not allow
 * for arrays to contain elements on one type.
 */
public class JsonSchemaFixedMixedArrayResolver {

    private final JsonSchemaTypeResolver resolver;

    public JsonSchemaFixedMixedArrayResolver() {
        this.resolver = new JsonSchemaTypeResolver();
    }

    /**
     * Parse a JsonNode containing JsonSchemas objects to extract the
     * type information needed to
     *
     * @param items Json array containing JsonSchema objects
     * @param refsVisited
     * @return
     */
    public List<RpcType> resolve(JsonNode items, TypeRegistry refsVisited) {
        List<RpcType> paramTypes = new LinkedList<>();

        if(items == null || ! items.isArray()) {
            throw new SchemaException("items must be an array.");
        }
        for(Iterator<JsonNode> it = items.elements(); it.hasNext(); ) {
            JsonNode param = it.next();
            NamedRpcType t = resolver.resolveNamedSchema(param, refsVisited);
            paramTypes.add(t);
        }

        return paramTypes;
    }
}
