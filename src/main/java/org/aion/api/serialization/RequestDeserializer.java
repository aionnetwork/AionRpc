package org.aion.api.serialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import org.aion.api.schema.JsonSchemaTypeResolver;

import java.io.IOException;
import java.util.List;

import org.aion.api.schema.SchemaValidationException;
import org.aion.api.schema.SchemaValidator;
import org.aion.api.schema.TypeRegistry;

public class RequestDeserializer {
    private final ObjectMapper om;
    protected final JsonSchemaTypeResolver resolver;
    private final JsonNode typesRoot;
    private final RpcMethodSchemaLoader schemaLoader;
    private final SchemaValidator validator;
    private final RpcTypeDeserializer deserializer;

    @VisibleForTesting
    RequestDeserializer(ObjectMapper om,
                        JsonNode typesRoot,
                        RpcMethodSchemaLoader schemaLoader,
                        SchemaValidator validator,
                        RpcTypeDeserializer deserializer) {
        this.om = om;
        this.resolver = new JsonSchemaTypeResolver();
        this.typesRoot = typesRoot;
        this.schemaLoader = schemaLoader;
        this.validator = validator;
        this.deserializer = deserializer;
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
        JsonNode reqRoot = schemaLoader.loadRequestSchema(method);

        List<JsonNode> schemaParamNodes = Lists.newArrayList(reqRoot.get("items").elements());
        List<JsonNode> paramNodes = Lists.newArrayList(params.elements());

        if(schemaParamNodes.size() != paramNodes.size()) {
            throw new SchemaValidationException(String.format(
                    "Wrong number of arguments (expected %d but got %d)",
                    schemaParamNodes.size(),
                    paramNodes.size()));
        }

        Object[] reqParams = new Object[paramNodes.size()];
        TypeRegistry tr = new TypeRegistry();

        for(int ix = 0; ix < paramNodes.size(); ++ix) {
            reqParams[ix] = deserializer.deserialize(
                    paramNodes.get(ix),
                    resolver.resolveNamedSchema(schemaParamNodes.get(ix)));
        }

        req.setParams(reqParams);
        return req;
    }
}