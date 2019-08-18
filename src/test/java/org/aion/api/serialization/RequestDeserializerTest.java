package org.aion.api.serialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;

import org.aion.api.codegen.GenerateDeserializer;
import org.aion.api.schema.*;
import org.junit.Test;

import javax.tools.*;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RequestDeserializerTest {
    private final ObjectMapper om = new ObjectMapper();
    private RpcMethodSchemaLoader schemaLoader = mock(
        RpcMethodSchemaLoader.class);
    private SchemaValidator validator = new SchemaValidator();
    private final JsonNode typesSchemaRoot;

    public RequestDeserializerTest() throws Exception {
        URL typesUrl = Resources.getResource("schemas/type/root.json");
        String types = Resources.toString(typesUrl, Charsets.UTF_8);
        typesSchemaRoot = om.readTree(types);
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
        when(schemaLoader.loadRequestSchema("testMethod"))
            .thenReturn(requestSchema);

        String payload = "{                                                                                                                                                                                                                   \n" +
            "  \"method\": \"testMethod\",\n" +
            "  \"params\": [\"0x10\", \"0xe\", true],\n" +
            "  \"id\": \"1\",\n" +
            "  \"jsonrpc\": \"2.0\"\n" +
            "}";
        RequestDeserializer unit = new RequestDeserializer(
            om,
            schemaLoader,
            new TestDeserializer()
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
        when(schemaLoader.loadRequestSchema("testMethod"))
            .thenReturn(requestSchema);

        String payload = "{                                                                                                                                                                                                                   \n" +
            "  \"method\": \"testMethod\",\n" +
            "  \"params\": [\"0x123456789a123456789a123456789a123456789a123456789a123456789a12345\"],\n" +
            "  \"id\": \"1\",\n" +
            "  \"jsonrpc\": \"2.0\"\n" +
            "}";
        RequestDeserializer unit = new RequestDeserializer(
                om,
                schemaLoader,
                new TestDeserializer()
        );

        try {
            JsonRpcRequest result = unit.deserialize(payload);
        } catch (SchemaValidationException svx) {

        }
    }

    @Test(expected = SchemaValidationException.class)
    public void testFailedLengthConstraint() throws Exception {
        JsonNode requestSchema = om.readTree(
            "{"
                + "\"type\": \"array\","
                + "\"items\" : "
                + "[ "
                + "{ \"$ref\" : \"derived.json#/definitions/DATA32\" } "
                + "]}");
        when(schemaLoader.loadRequestSchema("testMethod"))
            .thenReturn(requestSchema);

        String payload = "{                                                                                                                                                                                                                   \n" +
            "  \"method\": \"testMethod\",\n" +
            "  \"params\": [\"0x10\"],\n" +
            "  \"id\": \"1\",\n" +
            "  \"jsonrpc\": \"2.0\"\n" +
            "}";
        RequestDeserializer unit = new RequestDeserializer(
                om,
                schemaLoader,
                new TestDeserializer()
        );
        unit.deserialize(payload);
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
        when(schemaLoader.loadRequestSchema("testMethod"))
                .thenReturn(requestSchema);

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
                new TestDeserializer()
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

    @Test(expected = SchemaValidationException.class)
    public void testSimpleObjectFailingValidation() throws Exception {
        JsonNode requestSchema = om.readTree(
                "{"
                        + "\"type\": \"array\","
                        + "\"items\" : "
                        + "[ "
                        + "{ \"$ref\" : \"derived.json#/definitions/SomeStruct\" } "
                        + "]}");
        when(schemaLoader.loadRequestSchema("testMethod"))
                .thenReturn(requestSchema);

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
                new TestDeserializer()
        );
        unit.deserialize(payload);
    }

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