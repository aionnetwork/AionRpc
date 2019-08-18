package org.aion.api.serialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

public class RpcMethodSchemaLoader {
    private ObjectMapper om = new ObjectMapper();

    public JsonNode loadRequestSchema(String methodName) throws IOException {
        return SerializationUtils.loadSchema(
            om, "schemas/" + methodName + ".request.json");
    }

    public JsonNode loadResponseSchema(String methodName) throws IOException {
        return SerializationUtils.loadSchema(
            om, "schemas/" + methodName + ".response.json");
    }
}
