package org.aion.api.serialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import java.io.IOException;

import org.aion.api.schema.JsonSchemaTypeResolver;

import java.net.URL;

import static org.aion.api.serialization.Utils.bytesToHex;

public class ResponseSerializer {
    private final ObjectMapper om = new ObjectMapper();
    private final JsonSchemaTypeResolver resolver = new JsonSchemaTypeResolver();
    private final JsonNode typesRoot; // not used yet, will be needed when types other than DATA,QUANTITY are added

    public static void main(String[] args) throws Exception {
        URL typesUrl = Resources.getResource("schemas/type.json");
        String types = Resources.toString(typesUrl, Charsets.UTF_8);
        JsonNode typesSchemaRoot = new ObjectMapper().readTree(types);
        String x = new ResponseSerializer(typesSchemaRoot).serialize(
                new JsonRpcResponse(new byte[] { 0xa, 0xb }, "1.0"),
                "getseed" );
        System.out.println(x);
    }

    public ResponseSerializer(JsonNode typesRoot) {
        this.typesRoot = typesRoot;
    }

    public String serialize(JsonRpcResponse resp, String method) throws IOException {
        URL rezUrl = Resources.getResource("schemas/" + method + ".response.json");
        String rez = Resources.toString(rezUrl, Charsets.UTF_8);
        JsonNode rezRoot = om.readTree(rez);

        // TODO: Aggregate types not supported yet
        String jsonRetType = rezRoot.get("$ref").asText();
        // Handle the built-in json types DATA, QUANTITY
        if(jsonRetType.endsWith("DATA")|| jsonRetType.endsWith("QUANTITY")) {
            String resultJson = String.format("\"0x%s\"", bytesToHex( (byte[]) resp.getResult() ));
            // TODO: Jackson it
            return String.format("{"
                + "\"jsonrpc\": \"%s\","
                + "\"id\": \"%s\","
                + "\"result\": %s"
                + "}",
                resp.getJsonrpc(), resp.getId(), resultJson);
        } else {
            throw new UnsupportedOperationException("Can only deserialize DATA and QUANTITY currently.");
        }
    }


}
