package org.aion.api.schema;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * This resolver is for intended for resolving the schemas used in
 * the Aion Rpc error schema files (i.e. the ones in resources/schemas/METHOD.error.json).
 *
 * It is hard-coded to the structure we expected, which is that each of those error files
 * is a JsonSchema containing either:
 *   1) a single $ref to an error type (i.e. defined in resources/schemas/type/errors.json)
 *   2) an anyOf containing any number of $refs to an error type
 */
public class JsonSchemaErrorResolver {

    public List<ErrorUsage> resolve(JsonNode schema) {
        return resolve(schema, false);
    }

    private List<ErrorUsage> resolve(JsonNode schema, boolean inAnyOf) {
        //TODO error handling / bad input cases need work

        if(schema.has("$ref")) {
            // base case
            String[] refPieces = schema.get("$ref").asText().split("/");
            if (refPieces.length == 0) {
                throw new SchemaRestrictionException("Malformed $ref string in given schema: "
                        + schema.toString());
            }
            String errTypeName = refPieces[refPieces.length-1];
            String reason = null;
            if(schema.has("description")) {
                reason = schema.get("description").asText();
            }

            return List.of(new ErrorUsage(errTypeName, reason));
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
                    "Expected top-level schema to be $ref or anyOf, but it was: " +
                    schema.toString());
        }
    }
}
