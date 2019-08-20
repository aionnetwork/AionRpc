package org.aion.api.serialization;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigInteger;

import org.aion.api.RpcException;
import org.aion.api.codegen.GenerateDeserializer;
import org.aion.api.schema.*;
import org.json.JSONPointer;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class RequestDeserializerTest {
    private final ObjectMapper om = new ObjectMapper();
    private RpcSchemaLoader schemaLoader = spy(RpcSchemaLoader.class);

    public RequestDeserializerTest() throws IOException {
    }

    @Test
    public void testMixOfRootScalars() throws Exception {
        JsonNode requestSchema = om.readTree(
            "{"
                + "\"type\": \"array\","
                + "\"items\" : "
                + "[ "
                + "{ \"$ref\" : \"root.json#/definitions/DATA\" }, "
                + "{ \"$ref\" : \"root.json#/definitions/QUANTITY\" }, "
                + "{ \"type\" : \"boolean\" } "
                + "]}");
        // make the schema loader act as if there is a method called 'testMethod'
        // that uses requestSchema
        doReturn(requestSchema).when(
                schemaLoader).loadRequestSchema("testMethod");

        String payload = "{                                                                                                                                                                                                                   \n" +
            "  \"method\": \"testMethod\",\n" +
            "  \"params\": [\"0x10\", \"0xe\", true],\n" +
            "  \"id\": \"1\",\n" +
            "  \"jsonrpc\": \"2.0\"\n" +
            "}";
        RequestDeserializer unit = new RequestDeserializer(
            om,
            schemaLoader,
            new TestDeserializer(),
            new JsonSchemaTypeResolver()
        );
        JsonRpcRequest result = unit.deserialize(payload);

        assertThat(result.getMethod(), is("testMethod"));
        assertThat(result.getJsonrpc(), is("2.0"));
        assertThat(result.getId(), is("1"));
        assertThat(result.getParams().length, is(3));
        assertThat(result.getParams()[0], is(new byte[] { 0x10 }));
        assertThat(result.getParams()[1], is(new BigInteger("e", 16)));
        assertThat(result.getParams()[2], is(true));
    }

    @Test
    public void testPassedLengthConstraint() throws Exception {
        JsonNode requestSchema = om.readTree(
            "{"
                + "\"type\": \"array\","
                + "\"items\" : "
                + "[ "
                + "{ \"$ref\" : \"derived.json#/definitions/DATA32\" } "
                + "]}");
        // make the schema loader act as if there is a method called 'testMethod'
        // that uses requestSchema
        doReturn(requestSchema).when(
                schemaLoader).loadRequestSchema("testMethod");

        String payload = "{                                                                                                                                                                                                                   \n" +
            "  \"method\": \"testMethod\",\n" +
            "  \"params\": [\"0x123456789a123456789a123456789a123456789a123456789a123456789a1234\"],\n" +
            "  \"id\": \"1\",\n" +
            "  \"jsonrpc\": \"2.0\"\n" +
            "}";
        RequestDeserializer unit = new RequestDeserializer(
                om,
                schemaLoader,
                new TestDeserializer(),
                new JsonSchemaTypeResolver()
        );

        JsonRpcRequest result = unit.deserialize(payload);
        assertThat(result.getParams()[0],
                is(SerializationUtils.hexStringToByteArray(
                        "0x123456789a123456789a123456789a123456789a123456789a123456789a1234")));
    }

    @Test
    public void testFailedLengthConstraint() throws Exception {
        JsonNode requestSchema = om.readTree(
            "{"
                + "\"type\": \"array\","
                + "\"items\" : "
                + "[ "
                + "{ \"$ref\" : \"derived.json#/definitions/DATA32\" } "
                + "]}");
        // make the schema loader act as if there is a method called 'testMethod'
        // that uses requestSchema
        doReturn(requestSchema).when(
                schemaLoader).loadRequestSchema("testMethod");

        String payload = "{                                                                                                                                                                                                                   \n" +
            "  \"method\": \"testMethod\",\n" +
            "  \"params\": [\"0x10\"],\n" +
            "  \"id\": \"1\",\n" +
            "  \"jsonrpc\": \"2.0\"\n" +
            "}";
        RequestDeserializer unit = new RequestDeserializer(
                om,
                schemaLoader,
                new TestDeserializer(),
                new JsonSchemaTypeResolver()
        );
        try {
            unit.deserialize(payload);
        } catch (RpcException e) {
            assertThat(e.getCode(), is(RpcException.invalidParams("any").getCode()));
            return;
        }
        fail("exception wasn't thrown");
    }

    @Test
    public void testSimpleObject() throws Exception {
        JsonNode requestSchema = om.readTree(
                "{"
                        + "\"type\": \"array\","
                        + "\"items\" : "
                        + "[ "
                        + "{ \"$ref\" : \"derived.json#/definitions/SomeStruct\" } "
                        + "]}");
        // make the schema loader act as if there is a method called 'testMethod'
        // that uses requestSchema
        doReturn(requestSchema).when(
                schemaLoader).loadRequestSchema("testMethod");

        // make the schema loader act as if there is an Aion RPC type defined
        // at reference "derived.json#/definitions/SomeStruct" with the
        // JsonSchema of someStructTypeDef.
        JsonSchemaRef someStructTypeDef = new JsonSchemaRef(
                "derived.json#/definitions/SomeStruct");
        doReturn(someStructJsonSchema).when(
                schemaLoader).loadSchemaRef(someStructTypeDef);

        String payload = "{                                                                                                                                                                                                                   \n" +
                "  \"method\": \"testMethod\",\n" +
                "  \"params\": [{" +
                "     \"MyData\":\"0x80f8085aed722d176fb5cf83e94ef57261e764e335488dd2f9413b3f64d1caa7\", " +
                "     \"MyQuantity\":\"0x99\"" +
                "  }],\n" +
                "  \"id\": \"1\",\n" +
                "  \"jsonrpc\": \"2.0\"\n" +
                "}";
        RequestDeserializer unit = new RequestDeserializer(
                om,
                schemaLoader,
                new TestDeserializer(),
                new JsonSchemaTypeResolver(schemaLoader)
        );
        JsonRpcRequest result = unit.deserialize(payload);

        assertThat(result.getParams()[0] instanceof SomeStruct, is(true));
        assertThat(result.getParams().length, is(1));

        SomeStruct param0 = (SomeStruct)result.getParams()[0];

        assertThat(param0.getMyData(),
                is(SerializationUtils.hexStringToByteArray(
                        "0x80f8085aed722d176fb5cf83e94ef57261e764e335488dd2f9413b3f64d1caa7")));
        assertThat(param0.getMyQuantity(),
                is(BigInteger.valueOf(0x99)));
    }

    @Test
    public void testSimpleObjectFailingValidation() throws Exception {
        JsonNode requestSchema = om.readTree(
                "{"
                        + "\"type\": \"array\","
                        + "\"items\" : "
                        + "[ "
                        + "{ \"$ref\" : \"derived.json#/definitions/SomeStruct\" } "
                        + "]}");
        // make the schema loader act as if there is a method called 'testMethod'
        // that uses requestSchema
        doReturn(requestSchema).when(
                schemaLoader).loadRequestSchema("testMethod");

        // make the schema loader act as if there is an Aion RPC type defined
        // at reference "derived.json#/definitions/SomeStruct" with the
        // JsonSchema of someStructTypeDef.
        JsonSchemaRef someStructTypeDef = new JsonSchemaRef(
                "derived.json#/definitions/SomeStruct");
        doReturn(someStructJsonSchema).when(
                schemaLoader).loadSchemaRef(someStructTypeDef);

        String payload = "{                                                                                                                                                                                                                   \n" +
                "  \"method\": \"testMethod\",\n" +
                "  \"params\": [{" +
                "     \"MyData\":\"0x1\", " +
                "     \"MyQuantity\":\"0x99\"" +
                "  }],\n" +
                "  \"id\": \"1\",\n" +
                "  \"jsonrpc\": \"2.0\"\n" +
                "}";
        RequestDeserializer unit = new RequestDeserializer(
                om,
                schemaLoader,
                new TestDeserializer(),
                new JsonSchemaTypeResolver(schemaLoader)
        );


        try {
            unit.deserialize(payload);
        } catch (RpcException e) {
            assertThat(e.getCode(), is(RpcException.invalidParams("any").getCode()));
            return;
        }
        fail("Expected RpcException");


    }

    @Test
    public void testParseError() throws Exception {
        RequestDeserializer unit = new RequestDeserializer(
                om,
                schemaLoader,
                new TestDeserializer(),
                new JsonSchemaTypeResolver()
        );

        try {
            unit.deserialize("{");
        } catch (RpcException e) {
            assertThat(e.getCode(), is(RpcException.parseError("any").getCode()));
            return;
        }
        fail("exception wasn't thrown");
    }

    @Test
    public void testJsonRpcEnvelopeUnexpectedField() throws Exception {
        String payload = "{                                                                                                                                                                                                                   \n" +
                "  \"method\": \"testMethod\",\n" +
                "  \"params\": [\"0x10\", \"0xe\", true],\n" +
                "  \"somecrazyfield\": \"1\",\n" +
                "  \"jsonrpc\": \"2.0\"\n" +
                "}";
        RequestDeserializer unit = new RequestDeserializer(
                om,
                schemaLoader,
                new TestDeserializer(),
                new JsonSchemaTypeResolver()
        );
        try {
            unit.deserialize(payload);
        } catch (RpcException e) {
            assertThat(e.getCode(), is(RpcException.invalidRequest("any").getCode()));
            return;
        }
        fail("exception wasn't thrown");
    }

    @Test
    public void testJsonRpcEnvelopeMissingMethod() throws Exception {
        String payload = "{                                                                                                                                                                                                                   \n" +
                "  \"params\": [\"0x10\", \"0xe\", true],\n" +
                "  \"jsonrpc\": \"2.0\"\n" +
                "}";
        RequestDeserializer unit = new RequestDeserializer(
                om,
                schemaLoader,
                new TestDeserializer(),
                new JsonSchemaTypeResolver()
        );
        try {
            unit.deserialize(payload);
        } catch (RpcException e) {
            assertThat(e.getCode(), is(RpcException.invalidRequest("any").getCode()));
            return;
        }
        fail("exception wasn't thrown");
    }

    @Test
    public void testJsonRpcEnvelopeMissingParams() throws Exception {
        String payload = "{                                                                                                                                                                                                                   \n" +
                "  \"method\": \"testMethod\",\n" +
                "  \"jsonrpc\": \"2.0\"\n" +
                "}";
        RequestDeserializer unit = new RequestDeserializer(
                om,
                schemaLoader,
                new TestDeserializer(),
                new JsonSchemaTypeResolver()
        );
        try {
            unit.deserialize(payload);
        } catch (RpcException e) {
            assertThat(e.getCode(), is(RpcException.invalidRequest("any").getCode()));
            return;
        }
        fail("exception wasn't thrown");
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
     * @implNote In real usage (i.e. from the Aion kernel), the {@link RpcTypeDeserializer}
     * that is used is generated code outputted by the {@link GenerateDeserializer} program
     * from the template TemplatedDeserializer.java.ftl.  This is a test implementation for
     * unit testing and also serves as a reference for what that program should
     * output and how it interacts with {@link RequestDeserializer}.
     */
    private static class TestDeserializer extends RpcTypeDeserializer {
        @Override
        protected Object deserializeObject(JsonNode value,
                                           NamedRpcType type) throws SchemaValidationException {
            switch(type.getName()) {
                case "SomeStruct":
                    return new SomeStruct(
                            (byte[]) super.deserialize(
                                    value.get("MyData"),
                                    (NamedRpcType) type.getContainedFields().get(0).getType()

                            ),
                            (java.math.BigInteger) super.deserialize(
                                    value.get("MyQuantity"),
                                    (NamedRpcType) type.getContainedFields().get(1).getType()
                            )
                    );
                default: throw new UnsupportedOperationException(
                        "Unsupported type.");
            }
        }
    }

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