package org.aion.api.serialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.junit.Test;

import java.net.URL;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class ResponseSerializerTest {
    private final ObjectMapper om = new ObjectMapper();
    private final JsonNode typesSchemaRoot;

    public ResponseSerializerTest() throws Exception {
        URL typesUrl = Resources.getResource("schemas/type.json");
        String types = Resources.toString(typesUrl, Charsets.UTF_8);
        typesSchemaRoot = om.readTree(types);
    }

    @Test
    public void testSerializeDerivedScalar() throws Exception {
        JsonNode responseSchema = om.readTree(
            "{\"$ref\" : \"types.json#/definitions/QUANTITY\"} ");
        ResponseSerializer unit = new ResponseSerializer(typesSchemaRoot);
        String result = unit.serialize(
            new JsonRpcResponse(new byte[] { 0xa, 0xb }, "1.0"),
            responseSchema);

        JsonNode resultJson = om.readTree(result);
        assertThat(resultJson.get("result").asText(), is("0x0a0b"));
    }

    @Test
    public void testSerializeBoolean() throws Exception {
        JsonNode responseSchema = om.readTree(
                "{\"type\" : \"boolean\"} ");
        ResponseSerializer unit = new ResponseSerializer(typesSchemaRoot);
        String result = unit.serialize(
                new JsonRpcResponse(true, "1.0"),
                responseSchema);

        JsonNode resultJson = om.readTree(result);
        assertThat(resultJson.get("result").asText(), is("true"));
    }
}