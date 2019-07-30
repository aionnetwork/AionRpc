package org.aion.api.serialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import org.aion.api.schema.JsonSchemaTypeResolver;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.util.List;

import static org.aion.api.serialization.Utils.hexStringToByteArray;

public class RequestDeserializer {
    private final ObjectMapper om = new ObjectMapper();
    private final JsonSchemaTypeResolver resolver = new JsonSchemaTypeResolver();
    private final JsonNode typesRoot; // not used yet, will be needed when types other than DATA,QUANTITY are added
    private final RpcMethodSchemaLoader schemaLoader;

    public RequestDeserializer(JsonNode typesRoot) {
        this(typesRoot, new RpcMethodSchemaLoader());
    }

    @VisibleForTesting
    RequestDeserializer(JsonNode typesRoot,
                        RpcMethodSchemaLoader schemaLoader) {
        this.typesRoot = typesRoot;
        this.schemaLoader = schemaLoader;
    }

    /**
     * Deserialize a String of the JSON of a JsonRpc method call into a
     * Java representation.
     *
     * @param payload JSON representation of the JsonRpc method call
     * @return Java representation of the payload
     * @throws IOException if schema for the method name in the call can't be loaded
     */
    public JsonRpcRequest deserialize(String payload) throws IOException  {
        // note to self -- needs to get called by AbstractRpcServer in the kernel.  which will
        // need to look at the request schema so it can cast the params array correctly
        JsonRpcRequest req = om.readValue(payload, JsonRpcRequest.class);

        JsonNode payloadRoot = om.readTree(payload);
        JsonNode params = payloadRoot.get("params");
        if(params == null || !params.isArray()) {
            throw new IllegalArgumentException("Params array missing from request.");
        }

        String method = payloadRoot.get("method").asText();
        JsonNode rezRoot = schemaLoader.loadRequestSchema(method);

        List<JsonNode> schemaParamNodes = Lists.newArrayList(rezRoot.get("items").elements());
        List<JsonNode> paramNodes = Lists.newArrayList(params.elements());

        if(schemaParamNodes.size() != paramNodes.size()) {
            throw new IllegalArgumentException(String.format(
                    "Wrong number of arguments (expected %d but got %d)", schemaParamNodes.size(), paramNodes.size()));
        }

        Object[] reqParams = new Object[paramNodes.size()];

        for(int ix = 0; ix < paramNodes.size(); ++ix) {
            // TODO: only works with DATA and QUANTITY right now
            // TODO: add validator -- it just assumes the input is correct right now
            JsonNode expectedTypeSchema = schemaParamNodes.get(ix);

            if(expectedTypeSchema.has("type")
                && expectedTypeSchema.get("type").asText().equals("boolean")) {
                reqParams[ix] = paramNodes.get(ix).asBoolean();
            } else if(expectedTypeSchema.get("$ref").asText().endsWith("DATA")) {

                String nodeVal = paramNodes.get(ix).asText();
                if (!nodeVal.startsWith("0x") || nodeVal.length() % 2 != 0) {
                    throw new IllegalArgumentException(String.format(
                            "DATA needs to start with 0x and be of even length (given: %s)", nodeVal));
                }

                nodeVal = nodeVal.replaceFirst("0x", "");
                reqParams[ix] = hexStringToByteArray(nodeVal);

            } else if(expectedTypeSchema.get("$ref").asText().endsWith("QUANTITY")) {

                String nodeVal = paramNodes.get(ix).asText();
                if (!nodeVal.startsWith("0x")) {
                    throw new IllegalArgumentException(String.format(
                            " QUANTITY needs to start with 0x (given: %s)", nodeVal));
                }

                nodeVal = nodeVal.replaceFirst("0x", "");
                if(nodeVal.length() % 2 != 0) {
                    nodeVal = "0" + nodeVal;
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
