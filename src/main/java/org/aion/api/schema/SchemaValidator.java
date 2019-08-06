package org.aion.api.schema;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import com.google.common.annotations.VisibleForTesting;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaClient;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;

/**
 * TODO PROTOTYPE ONLY - DON'T USE YET!
 *
 * Performs validation of JSON against JsonSchemas.
 *
 * The real work of the validation is done by the org.everit.json.schema library,
 * which uses the org.json library for JSON representation.  Most of the code
 * in the AionRpc project uses Jackson for JSON representation, so most of the work
 * in this class is performing the correct conversions from Jackson to org.json
 * so it can be fed into the everit validator.
 */
public class SchemaValidator {
    private ObjectMapper om;

    public SchemaValidator() {
        om = new ObjectMapper();
        om.registerModule(new JsonOrgModule());
    }

    /**
     * Validates input against the given schema; i.e. does it match the rules
     * and constraints of the schema?
     *
     * @param schema schema (using org.json library)
     * @param input the input undergoing validation (using by Jackson library)
     * @return whether input is valid according to schema
     * @throws JsonProcessingException if schema parse error
     */
    public boolean validate(JsonNode schema,
                            JsonNode input) throws JsonProcessingException {
        JSONObject castedSchema = om.treeToValue(schema, JSONObject.class);
        return validate(castedSchema, input);
    }

    /**
     * @implNote Generally, should use {@link #validate(JsonNode, JsonNode)}
     * instead; its signature uses Jackson lib classes only, which is what
     * the rest of AionRpc uses.
     *
     * @param schema schema (using org.json library)
     * @param input the input undergoing validation (using by Jackson library)
     * @return whether input is valid according to schema
     * @throws JsonProcessingException if schema parse error
     */
    @VisibleForTesting
    boolean validate(JSONObject schema,
                     JsonNode input)
    throws JsonProcessingException {
        Schema validator = buildSchema(schema);
        final Object inputObj;

        // The type of the object is used by the everit validator to
        // determine the rules of the validation, but it can't take
        // JsonNode as input because that comes from Jackson lib, which
        // it doesn't understand.
        switch(input.getNodeType()) {
            case STRING:
                // the validator treats Java strings as Javascript strings
                // i.e. matches schema {"type": "string"}
                inputObj = om.treeToValue(input, String.class);
                break;
            case BOOLEAN:
                inputObj = om.treeToValue(input, Boolean.class);
                break;
            case OBJECT:
                // the validator treats Java objects as Javascript objects
                // i.e. matches schema {"type": "object"}
                // TODO: double check that this conversion makes sense for all cases
                inputObj = om.treeToValue(input, JSONObject.class);
                break;
            case ARRAY: // not yet supported
            case NUMBER: // intentionally not supported
            default:
                throw new UnsupportedOperationException(String.format(
                    "Don't know how to convert the Json node type '%s'.  The node was: %s",
                    input.getNodeType().toString(), input.toString()));
        }

        try {
            validator.validate(inputObj);
            return true;
        } catch (ValidationException vx) {
            return false;
        }
    }

    private Schema buildSchema(JSONObject schema) {
        SchemaLoader schemaLoader = SchemaLoader.builder()
            .schemaClient(SchemaClient.classPathAwareClient())
            .schemaJson(schema)
            .resolutionScope("file:///home/sergiu/repos/aion_AionRpc/src/main/resources/schemas/type/")
            .build();
        return schemaLoader.load().build();
    }
}
