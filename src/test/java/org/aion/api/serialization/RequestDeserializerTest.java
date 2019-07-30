package org.aion.api.serialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import java.math.BigInteger;
import java.net.URL;
import org.aion.api.schema.JsonSchemaTypeResolver;
import org.aion.api.schema.SchemaValidator;
import org.junit.Test;

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
        URL typesUrl = Resources.getResource("schemas/type.json");
        String types = Resources.toString(typesUrl, Charsets.UTF_8);
        typesSchemaRoot = om.readTree(types);
    }

    @Test
    public void testMixOfScalars() throws Exception {
        JsonNode requestSchema = om.readTree(
            "{"
                + "\"type\": \"array\","
                + "\"items\" : "
                + "[ "
                + "{ \"$ref\" : \"type.json#/definitions/DATA\" }, "
                + "{ \"$ref\" : \"type.json#/definitions/QUANTITY\" }, "
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
            typesSchemaRoot,
            schemaLoader,
            validator);
        JsonRpcRequest result = unit.deserialize(payload);

        assertThat(result.getMethod(), is("testMethod"));
        assertThat(result.getJsonrpc(), is("2.0"));
        assertThat(result.getId(), is("1"));
        assertThat(result.getParams().length, is(3));
        assertThat(result.getParams()[0], is(new byte[] { 0x10 }));
        assertThat(result.getParams()[1], is(new BigInteger("e", 16)));
        assertThat(result.getParams()[2], is(true));
    }
}