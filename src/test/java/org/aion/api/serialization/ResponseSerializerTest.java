package org.aion.api.serialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.aion.api.schema.JsonSchemaRef;
import org.aion.api.schema.JsonSchemaTypeResolver;
import org.aion.api.schema.SchemaValidationException;
import org.junit.Test;

import java.math.BigInteger;
import java.net.URL;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ResponseSerializerTest {
    private final ObjectMapper om = new ObjectMapper();
    private RpcSchemaLoader schemaLoader = mock(RpcSchemaLoader.class);
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

        ResponseSerializer unit = new ResponseSerializer(
                new JsonSchemaTypeResolver(),
                schemaLoader);

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

        ResponseSerializer unit = new ResponseSerializer(
                new JsonSchemaTypeResolver(),
                schemaLoader);

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

        ResponseSerializer unit = new ResponseSerializer(
                new JsonSchemaTypeResolver(),
                schemaLoader);
        String result = unit.serialize(
                new JsonRpcResponse(true, "1.0"),
                "testMethod");

        JsonNode resultJson = om.readTree(result);
        assertThat(resultJson.get("result").asText(), is("true"));
    }

    @Test
    public void testSerializeSimpleObject() throws Exception {
        // use spy for this one because we want the schema loader to
        // take on its normal behaviour in the non-stubbed methods so
        // it can load the real (production) types that are nested inside
        // our "SomeStruct" test type
        schemaLoader = spy(RpcSchemaLoader.class);

        JsonNode responseSchema = om.readTree(
                "{\"$ref\" : \"derived.json#/definitions/SomeStruct\"} ");
        // make the schema loader act as if there is a method called 'testMethod'
        // that has response schema
        doReturn(responseSchema).when(
                schemaLoader).loadResponseSchema("testMethod");

        // make the schema loader act as if there is an Aion RPC type defined
        // at reference "derived.json#/definitions/SomeStruct" with the
        // JsonSchema of someStructTypeDef.
        JsonSchemaRef someStructTypeDef = new JsonSchemaRef(
                "derived.json#/definitions/SomeStruct");
        doReturn(someStructJsonSchema).when(
                schemaLoader).loadSchemaRef(someStructTypeDef);

        ResponseSerializer unit = new ResponseSerializer(
                new JsonSchemaTypeResolver(schemaLoader),
                schemaLoader);
        String result = unit.serialize(
                new JsonRpcResponse(new SomeStruct(
                        SerializationUtils.hexStringToByteArray("0x68d1d3bffe8672cf1e9e85fbdb9f62744ccf7d7ac5848e7d46441169db99112a"),
                        BigInteger.valueOf(1337)
                ), "1.0"),
                "testMethod");

        // can't just compare the whole string because ordering
        // of object fields aren't fixed
        JsonNode resultJson = om.readTree(result);
        assertThat(resultJson.get("myQuantity").asText(), is("0x539"));
        assertThat(resultJson.get("myData").asText(),
                is("0x68d1d3bffe8672cf1e9e85fbdb9f62744ccf7d7ac5848e7d46441169db99112a"));

    }

    // -- SomeStruct set up -------------------------------------------------------------
    /**
     * @implNote We do this so we don't have to put 'SomeStruct' into the production
     * types JsonSchemas (don't want to generate code for it for the real library
     * during build time since it's for test only), but also don't want to depend
     * on the definitions in the production types definitions.
     */
    private JsonNode someStructJsonSchema = om.readTree(
            "     {\n" +
                    "      \"$comment\": \"Test structure used only by unit tests.\",\n" +
                    "      \"type\": \"object\",\n" +
                    "      \"properties\": {\n" +
                    "        \"MyData\": {\"$ref\": \"derived.json#/definitions/DATA32\"},\n" +
                    "        \"MyQuantity\": {\"$ref\": \"root.json#/definitions/QUANTITY\"}\n" +
                    "      }\n" +
                    "    }"
    );

    /**
     * @implNote In real usage (i.e. from the Aion kernel), POD types are generated by the
     * {@link org.aion.api.codegen.GenerateDataHolders} program.  This is a test POD type
     * for unit testing and also serves as a reference for what that program should
     * output and how it interacts with {@link RequestDeserializer}.
     */
    private static class SomeStruct {
        private byte[] MyData;
        private java.math.BigInteger MyQuantity;

        public SomeStruct(
                byte[] MyData,
                java.math.BigInteger MyQuantity
        ) {
            this.MyData = MyData;
            this.MyQuantity = MyQuantity;
        }

        public byte[] getMyData() {
            return this.MyData;
        }

        public java.math.BigInteger getMyQuantity() {
            return this.MyQuantity;
        }
    }


}