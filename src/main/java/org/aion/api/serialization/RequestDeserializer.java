package org.aion.api.serialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    public static void main(String[] args) throws Exception {
        String payload = "{                                                                                                                                                                                                                   \n" +
                "  \"method\": \"submitseed\",\n" +
                "  \"params\": [\"0x10\", \"0xee\"],\n" +
                "  \"id\": \"1\",\n" +
                "  \"jsonrpc\": \"2.0\"\n" +
                "}";

        JsonRpcRequest req = new RequestDeserializer(null).deserialize(payload);

        System.out.println(req.getParams());
    }

    public RequestDeserializer(JsonNode typesRoot) {
        this.typesRoot = typesRoot;
    }

    public JsonRpcRequest deserialize(String payload) throws IOException  {
        // note to self -- needs to get called by AbstractRpcServer in the kernel.  which will
        // need to look at the request schema so it can cast the params array correctly
        JsonRpcRequest req = om.readValue(payload, JsonRpcRequest.class);


        JsonNode payloadRoot = om.readTree(payload);
        JsonNode params = payloadRoot.get("params");
        if(params == null || !params.isArray()) {
            throw new IllegalArgumentException("Params array missing from request.");
        }

        URL rezUrl = Resources.getResource("schemas/" + payloadRoot.get("method").asText() + ".request.json");
        String rez = Resources.toString(rezUrl, Charsets.UTF_8);
        JsonNode rezRoot = om.readTree(rez);

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

            if(expectedTypeSchema.isBoolean()) {
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
