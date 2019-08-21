package org.aion.api.serialization;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import org.aion.api.RpcException;
import org.aion.api.schema.JsonSchemaTypeResolver;

import java.io.IOException;
import java.util.List;

import org.aion.api.schema.SchemaValidationException;

public class RequestDeserializer {
    private final ObjectMapper om;
    protected final JsonSchemaTypeResolver resolver;
    private final RpcSchemaLoader schemaLoader;
    private final RpcTypeDeserializer deserializer;

    public RequestDeserializer(RpcTypeDeserializer rpcTypeDeserializer) {
        this(
                new ObjectMapper(),
                new RpcSchemaLoader(),
                rpcTypeDeserializer,
                new JsonSchemaTypeResolver()
        );
    }

    @VisibleForTesting
    RequestDeserializer(ObjectMapper om,
                        RpcSchemaLoader schemaLoader,
                        RpcTypeDeserializer deserializer,
                        JsonSchemaTypeResolver resolver) {
        this.om = om;
        this.resolver = resolver;
        this.schemaLoader = schemaLoader;
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
    throws RpcException, IOException {

        final JsonRpcRequest req;
        try {
            req = om.readValue(payload, JsonRpcRequest.class);
        } catch(JsonParseException jpe) {
            // JSON parse error
            throw RpcException.parseError(jpe.getMessage());
        } catch (JsonMappingException jme) {
            // happens if the fields in the json don't match up to
            // the jsonrpc envelope
            throw RpcException.invalidRequest(jme.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }

        JsonNode payloadRoot = om.readTree(payload);
        JsonNode params = payloadRoot.get("params");
        if(params == null || !params.isArray()) {
            throw RpcException.invalidRequest("Missing params field in request");
        }

        String method = payloadRoot.get("method").asText();
        JsonNode reqRoot = schemaLoader.loadRequestSchema(method);

        List<JsonNode> schemaParamNodes = Lists.newArrayList(reqRoot.get("items").elements());
        List<JsonNode> paramNodes = Lists.newArrayList(params.elements());

        if(schemaParamNodes.size() != paramNodes.size()) {
            throw RpcException.invalidParams(String.format(
                    "Wrong number of arguments (expected %d but got %d)",
                    schemaParamNodes.size(),
                    paramNodes.size()));
        }

        Object[] reqParams = new Object[paramNodes.size()];

        for(int ix = 0; ix < paramNodes.size(); ++ix) {
            try {
                reqParams[ix] = deserializer.deserialize(
                        paramNodes.get(ix),
                        resolver.resolveNamedSchema(schemaParamNodes.get(ix)));
            } catch (SchemaValidationException svx) {
                throw RpcException.invalidParams(svx.getMessage()); // TODO can we improve the info that's surfaced?
            }
        }

        req.setParams(reqParams);
        return req;
    }

    /**
     * Try to serialize the request json payload and get the id.  If anything
     * fails, return null instead.  Intended for handling malformed requests
     * as a best-effort to get some sort of id to attach to the error response.
     */
    public String idOfRequest(String requestJsonPayload) {
        try {
            JsonNode req = om.readTree(requestJsonPayload);
            if(req == null) {
                return null;
            }
            JsonNode id = req.get("id");
            if(id == null) {
                return null;
            }
            return id.asText();
        } catch (IOException ex) {
            return null;
        }
    }
}