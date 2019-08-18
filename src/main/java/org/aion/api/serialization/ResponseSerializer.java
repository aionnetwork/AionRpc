package org.aion.api.serialization;

import static org.aion.api.serialization.SerializationUtils.bytesToHex;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.math.BigInteger;

import org.aion.api.schema.JsonSchemaTypeResolver;
import org.aion.api.schema.RootTypes;
import org.aion.api.schema.RpcType;
import org.aion.api.schema.SchemaValidationException;
import org.aion.api.schema.SchemaValidator;

public class ResponseSerializer {
    private final ObjectMapper om = new ObjectMapper();
    private final JsonSchemaTypeResolver resolver = new JsonSchemaTypeResolver();
    private final RpcMethodSchemaLoader schemaLoader;
    private final SchemaValidator validator = new SchemaValidator();

    public ResponseSerializer(JsonNode typesRoot) {
        this(typesRoot, new RpcMethodSchemaLoader());
    }

    @VisibleForTesting
    public ResponseSerializer(JsonNode typesRoot,
                              RpcMethodSchemaLoader schemaLoader) {
        this.schemaLoader = schemaLoader;

        SimpleModule customSerializers = new SimpleModule();
        customSerializers.addSerializer(byte[].class, new BytesSerializer());
        customSerializers.addSerializer(BigInteger.class, new BigIntSerializer());
        om.registerModule(customSerializers);
    }

    /**
     * Serialize the response of a JsonRpc method to a String of that JSON,
     * so that it may be sent back to the JsonRpc caller
     *
     * @param resp JSON response to serialize
     * @param method Name of the JsonRpc method that this response is for
     * @return String representation of given response
     * @throws IOException if JsonSchema of the given method could not be loaded
     */
    public String serialize(JsonRpcResponse resp, String method)
    throws IOException, SchemaValidationException {
        JsonNode responseSchema = schemaLoader.loadResponseSchema(method);

        if(responseSchema.get("$ref") != null) {
            RpcType type = resolver.resolveSchema(responseSchema);

            if (type.getRootType().equals(RootTypes.DATA)
                || type.getRootType().equals(RootTypes.QUANTITY)) {

                String resultJson = String.format("\"0x%s\"", bytesToHex((byte[]) resp.getResult()));
                if(! validator.validate(responseSchema, om.readTree(resultJson))) {
                    throw new SchemaValidationException(String.format(
                        "Response data did not conform to the schema for the requested RPC method.  "
                            + "This is a bug in either the Response Serializer or RPC method implementation.  "
                            + "method: %s.  Response data: %s",
                        method,
                        resultJson
                    ));
                }

                // TODO: Jackson it
                return String.format("{"
                        + "\"jsonrpc\": \"%s\","
                        + "\"id\": \"%s\","
                        + "\"result\": %s"
                        + "}",
                    resp.getJsonrpc(), resp.getId(), resultJson);
            } else {
                // TODO not tested/used yet
                // Custom types -- these are custom classes that will have Jackson annotations
                return om.writeValueAsString(resp.getResult());
            }

        } else if (responseSchema.get("type") != null) {
            String typeString = responseSchema.get("type").asText();
            switch(typeString) {
                case "boolean":
                    String resultJson = (boolean) resp.getResult() ? "true" : "false";
                    return String.format("{"
                            + "\"jsonrpc\": \"%s\","
                            + "\"id\": \"%s\","
                            + "\"result\": %s"
                            + "}",
                        resp.getJsonrpc(), resp.getId(), resultJson);
                case "number": // not used
                case "string": // not used
                case "struct": // not used
                case "array": // not used
                    throw new UnsupportedOperationException(
                        "Don't know how to serialize return type " + typeString);
            }
        }

        throw new UnsupportedOperationException(
            "Don't know how to serialize return type.  Return type schema: "
                + responseSchema.toString());
    }

    private static class BigIntSerializer extends StdSerializer<BigInteger> {
        public BigIntSerializer() {
            this(null);
        }

        public BigIntSerializer(Class<BigInteger> t) {
            super(t);
        }

        @Override
        public void serialize(BigInteger value,
                              JsonGenerator gen,
                              SerializerProvider provider)
        throws IOException {
            gen.writeString("0x" + value.toString(16));
        }
    }

    private static class BytesSerializer extends StdSerializer<byte[]> {
        public BytesSerializer() {
            this(null);
        }

        public BytesSerializer(Class<byte[]> t) {
            super(t);
        }

        @Override
        public void serialize(byte[] value,
                              JsonGenerator gen,
                              SerializerProvider provider)
        throws IOException {
            gen.writeString("0x" + bytesToHex(value));
        }
    }
}
