package org.aion.api.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import org.aion.api.serialization.RpcSchemaLoader;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * This resolver is for intended for resolving the schemas used in
 * on the 'error' block of Aion RPC method schemas.
 *
 * It is hard-coded to the structure we expected, which is that each of those error files
 * is a JsonSchema containing either:
 *   1) a single $ref to an error type (i.e. defined in resources/schemas/type/errors.json)
 *   2) an anyOf containing any number of $refs to an error type
 */
public class JsonSchemaErrorResolver {
    private final RpcSchemaLoader loader;

    public JsonSchemaErrorResolver() {
        this(new RpcSchemaLoader());
    }

    @VisibleForTesting JsonSchemaErrorResolver(RpcSchemaLoader loader) {
        this.loader = loader;
    }

    public List<ErrorUsage> resolve(JsonNode schema) {
        return resolve(schema, false);
    }

    private List<ErrorUsage> resolve(JsonNode schema, boolean inAnyOf) {
        if(schema.size() == 0) {
            return List.of();
        } else if(schema.has("$ref")) {
            // base case
            JsonSchemaRef ref = new JsonSchemaRef(schema.get("$ref").asText());
            String reason = null;
            if(schema.has("description")) {
                reason = schema.get("description").asText();
            }

            return List.of(
                    new ErrorUsage(resolveError(ref), reason));
        } else if (schema.has("anyOf")) {
            // recursive case -- only allowed to recurse once (can't nest anyOfs)
            if(inAnyOf) {
                throw new SchemaRestrictionException(
                        "Not allowed to nest anyOfs in schema.  Given schema: "
                                + schema.toString());
            }
            LinkedList<ErrorUsage> errors = new LinkedList<>();

            JsonNode anyOf = schema.get("anyOf");
            for(Iterator<JsonNode> it = anyOf.elements(); it.hasNext(); ) {
                JsonNode curr = it.next();
                ErrorUsage errUsage = resolve(curr, true)
                        .get(0); // since elements must be scalar
                errors.add(errUsage);
            }
            return errors;
        } else {
            throw new SchemaRestrictionException(
                    "Expected schema to contain $ref or anyOf, but schema was: " +
                    schema.toString());
        }
    }

    private RpcError resolveError(JsonSchemaRef errorRef) {
        JsonNode definition;
        try {
            definition = loader.loadType(errorRef);
        } catch (IOException ioe) {
            throw new SchemaException(String.format(
                    "Failed to load schema file '%s' when dereferencing pointer '%s'",
                    errorRef.getFile(),
                    errorRef.getValue())
            );
        }

        int code = definition.get("properties").get("code").get("const").asInt();
        String message = definition.get("properties").get("message").get("const").asText();
        return new RpcError(errorRef.getTypeName(), code, message);
    }
}
