package org.aion.api.serialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import org.aion.api.schema.JsonSchemaRef;
import org.aion.api.schema.JsonSchemaTypeResolver;

import java.io.IOException;

import static org.aion.api.serialization.Utils.bytesToHex;

public class ResponseSerializer {
    private final ObjectMapper om = new ObjectMapper();
    private final JsonSchemaTypeResolver resolver = new JsonSchemaTypeResolver();
    private final JsonNode typesRoot; // not used yet, will be needed when types other than DATA,QUANTITY are added

    public ResponseSerializer(JsonNode typesRoot) {
        this.typesRoot = typesRoot;
    }

    @VisibleForTesting
    String serialize(JsonRpcResponse resp, JsonNode responseSchema) throws IOException {
        if(responseSchema.get("$ref") != null) {
            String refText = responseSchema.get("$ref").asText();
            JsonSchemaRef retType = new JsonSchemaRef(refText);

            if (retType.getTypeName().equals("DATA")
                    || retType.getTypeName().equals("QUANTITY")) {
                // Handle the built-in json types DATA, QUANTITY directly
                String resultJson = String.format("\"0x%s\"", bytesToHex((byte[]) resp.getResult()));
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

    public String serialize(JsonRpcResponse resp, String method) throws IOException {
        JsonNode responseSchema = Utils.loadSchema(om, "schemas/" + method + ".response.json");
        return serialize(resp, responseSchema);
    }


}
