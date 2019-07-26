package org.aion.api.schema;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
     *
     *
     * @param items
     * @param refsVisited
     * @return
     */
    public List<ParamType> resolve(JsonNode items, JsonReferences refsVisited) {
        List<ParamType> paramTypes = new LinkedList<>();

        if(items == null || ! items.isArray()) {
            throw new SchemaException("items must be an array.");
        }
        for(Iterator<JsonNode> it = items.elements(); it.hasNext(); ) {
            JsonNode param = it.next();
            ParamType t = resolver.resolve(param, refsVisited);
            paramTypes.add(t);
        }

        return paramTypes;
    }
}
