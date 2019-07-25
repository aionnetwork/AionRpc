package org.aion.api.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import java.io.IOException;
import org.aion.api.envelope.JsonRpcResponse;

import java.net.URL;

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

    // lifted from https://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java
    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
}
