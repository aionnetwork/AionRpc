package org.aion.api.schema;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.junit.Test;

public class SchemaValidatorTest {
    private ObjectMapper om = new ObjectMapper();

    @Test
    public void validateString() throws Exception {
        JSONObject schema = new JSONObject("{\"type\": \"string\"}");
        SchemaValidator unit = new SchemaValidator();
        JsonNode input;

        // boolean against string: should fail
        input = om.readTree("false");
        assertThat(unit.validate(schema, input), is(false));

        // object against string: should fail
        input = om.readTree("{}");
        assertThat(unit.validate(schema, input), is(false));

        // string against string: should pass
        input = om.readTree("\"i feel so validated\"");
        assertThat(unit.validate(schema, input), is(true));
    }

    @Test
    public void validateRegexConstrainedString() throws Exception {
        JSONObject schema = new JSONObject("{\"$ref\": \"type.json#/definitions/DATA\"}");
        SchemaValidator unit = new SchemaValidator();
        JsonNode input;

        // boolean against string: should fail
        input = om.readTree("false");
        assertThat(unit.validate(schema, input), is(false));

        // object against string: should fail
        input = om.readTree("{}");
        assertThat(unit.validate(schema, input), is(false));

        // string against string failing constraint: should pass
        input = om.readTree("\"totally incorrect\"");
        assertThat(unit.validate(schema, input), is(false));
        input = om.readTree("\"0x1\"");
        assertThat(unit.validate(schema, input), is(false));

        // correct regex
        input = om.readTree("\"0x12\"");
        assertThat(unit.validate(schema, input), is(true));
    }
}