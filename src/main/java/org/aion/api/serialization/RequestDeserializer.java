package org.aion.api.serialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import org.aion.api.schema.RootTypes;
import org.aion.api.schema.JsonSchemaTypeResolver;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import org.aion.api.schema.NamedRpcType;
import org.aion.api.schema.RpcType;
import org.aion.api.schema.SchemaValidationException;
import org.aion.api.schema.SchemaValidator;
import org.aion.api.schema.TypeRegistry;

import static org.aion.api.serialization.Utils.hexStringToByteArray;

public class RequestDeserializer {
    private final ObjectMapper om;
    private final JsonSchemaTypeResolver resolver;
    private final JsonNode typesRoot;
    private final RpcMethodSchemaLoader schemaLoader;
    private final SchemaValidator validator;

    /**
     *
     *
     * @param typesRoot Root of a JSON schema structure that is a valid JsonSchema.
     * Expected to contain an object named {@code definitions} that contains
     * subschemas of all types that will be referenced (via JsonSchema keyword "$ref").
     */
    public RequestDeserializer(JsonNode typesRoot) {
        this(new ObjectMapper(),
            typesRoot,
            new RpcMethodSchemaLoader(),
            new SchemaValidator()
        );
    }

    @VisibleForTesting
    RequestDeserializer(ObjectMapper om,
                        JsonNode typesRoot,
                        RpcMethodSchemaLoader schemaLoader,
                        SchemaValidator validator) {
        this.om = om;
        this.resolver = new JsonSchemaTypeResolver();
        this.typesRoot = typesRoot;
        this.schemaLoader = schemaLoader;
        this.validator = validator;
    }

    /**
     * Deserialize a String of the JSON of a JsonRpc method call into a
     * Java representation.
     *
     * @param payload JSON representation of the JsonRpc method call
     * @return Java representation of the payload
     * @throws IOException if schema for the method name in the call can't be loaded
     */
    public JsonRpcRequest deserialize(String payload)
    throws IOException, SchemaValidationException {
        // note to self -- needs to get called by AbstractRpcServer in the kernel.  which will
        // need to look at the request schema so it can cast the params array correctly
        JsonRpcRequest req = om.readValue(payload, JsonRpcRequest.class);

        JsonNode payloadRoot = om.readTree(payload);
        JsonNode params = payloadRoot.get("params");
        if(params == null || !params.isArray()) {
            throw new SchemaValidationException("Params array missing from request.");
        }

        String method = payloadRoot.get("method").asText();
        JsonNode rezRoot = schemaLoader.loadRequestSchema(method);

        List<JsonNode> schemaParamNodes = Lists.newArrayList(rezRoot.get("items").elements());
        List<JsonNode> paramNodes = Lists.newArrayList(params.elements());

        if(schemaParamNodes.size() != paramNodes.size()) {
            throw new SchemaValidationException(String.format(
                    "Wrong number of arguments (expected %d but got %d)", schemaParamNodes.size(), paramNodes.size()));
        }

        Object[] reqParams = new Object[paramNodes.size()];
        TypeRegistry tr = new TypeRegistry();

        for(int ix = 0; ix < paramNodes.size(); ++ix) {
            // TODO: only works with DATA and QUANTITY right now
            JsonNode expectedTypeSchema = schemaParamNodes.get(ix);

            // Check this param value against the schema for the param
            if(! validator.validate(expectedTypeSchema, paramNodes.get(ix))) {
                throw new SchemaValidationException(
                    String.format("Schema validation error at parameter '%s'",
                        paramNodes.get(ix).toString()));
            }

            NamedRpcType rpcType = resolver.resolveNamedSchema(expectedTypeSchema, tr);
            RpcType root = rpcType.getRootType();

            // For everything type except those rooted in Object, the serialization
            // procedure is the same as their root type.  Just the validation part
            // is different, which has already happened.

            if(root.equals(RootTypes.BOOLEAN)) {

                reqParams[ix] = paramNodes.get(ix).asBoolean();

            } else if(root.equals(RootTypes.DATA)) {

                String nodeVal = paramNodes.get(ix).asText();
                reqParams[ix] = hexStringToByteArray(nodeVal);

            } else if(root.equals(RootTypes.QUANTITY)) {

                String nodeVal = paramNodes.get(ix).asText();
                // need to pad it to even-length so it may be converted to byte[]
                if(nodeVal.length() % 2 != 0) {
                    nodeVal = nodeVal.replaceFirst("0x", "0x0");
                }
                reqParams[ix] = new BigInteger(hexStringToByteArray(nodeVal));

            } else {
                throw new UnsupportedOperationException(
                        "Only DATA and QUANTITY types supported currently.");
            }
        }

        req.setParams(reqParams);
        return req;
    }
}
