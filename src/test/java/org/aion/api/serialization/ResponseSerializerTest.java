package org.aion.api.serialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.aion.api.schema.SchemaValidationException;
import org.junit.Test;

import java.math.BigInteger;
import java.net.URL;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResponseSerializerTest {
    private final ObjectMapper om = new ObjectMapper();
    private RpcMethodSchemaLoader schemaLoader = mock(RpcMethodSchemaLoader.class);
    private final JsonNode typesSchemaRoot;


    public ResponseSerializerTest() throws Exception {
        URL typesUrl = Resources.getResource("schemas/type/root.json");
        String types = Resources.toString(typesUrl, Charsets.UTF_8);
        typesSchemaRoot = om.readTree(types);
    }

    @Test
    public void testSerializeDerivedScalarWhenSchemaOk() throws Exception {
        JsonNode responseSchema = om.readTree(
            "{\"$ref\" : \"derived.json#/definitions/DATA32\"} ");
        when(schemaLoader.loadResponseSchema("testMethod"))
            .thenReturn(responseSchema);

        ResponseSerializer unit = new ResponseSerializer(typesSchemaRoot, schemaLoader);

        byte[] responseBytes = SerializationUtils.hexStringToByteArray("0xd6b391704355efdd37c5630638dc4d3798fc8fa98d60a0c02f45f0aa988e641f");
        String result = unit.serialize(
            new JsonRpcResponse(responseBytes, "1.0"), "testMethod");

        JsonNode resultJson = om.readTree(result);
        assertThat(resultJson.get("result").asText(), is("0xd6b391704355efdd37c5630638dc4d3798fc8fa98d60a0c02f45f0aa988e641f"));
    }

    @Test(expected = SchemaValidationException.class)
    public void testSerializeDerivedScalarWhenFailingSchemaCheck() throws Exception {
        JsonNode responseSchema = om.readTree(
            "{\"$ref\" : \"derived.json#/definitions/DATA32\"} ");
        when(schemaLoader.loadResponseSchema("testMethod"))
            .thenReturn(responseSchema);

        ResponseSerializer unit = new ResponseSerializer(typesSchemaRoot, schemaLoader);

        byte[] responseBytes = SerializationUtils.hexStringToByteArray("0x12");
        String result = unit.serialize(
            new JsonRpcResponse(responseBytes, "1.0"), "testMethod");

        om.readTree(result);
    }

    @Test
    public void testSerializeBoolean() throws Exception {
        JsonNode responseSchema = om.readTree(
                "{\"type\" : \"boolean\"} ");
        when(schemaLoader.loadResponseSchema("testMethod"))
            .thenReturn(responseSchema);

        ResponseSerializer unit = new ResponseSerializer(typesSchemaRoot, schemaLoader);
        String result = unit.serialize(
                new JsonRpcResponse(true, "1.0"),
                "testMethod");

        JsonNode resultJson = om.readTree(result);
        assertThat(resultJson.get("result").asText(), is("true"));
    }

    @Test
    public void testSerializeObjectWithBigInt() throws Exception {
        JsonNode responseSchema = om.readTree(
                "{\"$ref\" : \"derived.json#/definitions/SomeStruct\"} ");
        when(schemaLoader.loadResponseSchema("testMethod"))
                .thenReturn(responseSchema);
        class SomeClass {
            BigInteger bigInt = BigInteger.valueOf(15);

            public BigInteger getBigInt() {
                return bigInt;
            }
        }


        ResponseSerializer unit = new ResponseSerializer(typesSchemaRoot, schemaLoader);
        String result = unit.serialize(
                new JsonRpcResponse(new SomeClass(), "1.0"),
                "testMethod");

        System.out.println(result);

    }
}