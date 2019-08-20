package org.aion.api.serialization;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.aion.api.schema.JsonSchemaRef;
import org.aion.api.schema.SchemaException;

import java.io.IOException;
import java.net.URL;

/**
 * Loads files containing RPC-related definitions -- RPC method request/response
 * schemas and Aion RPC type schemas.
 */
public class RpcSchemaLoader {
    private ObjectMapper om = new ObjectMapper();

    /** Load schema for RPC method request */
    public JsonNode loadRequestSchema(String methodName) throws IOException {
        return SerializationUtils.loadSchemaRef(
            om, "schemas/" + methodName + ".request.json");
    }

    /** Load schema for RPC method response */
    public JsonNode loadResponseSchema(String methodName) throws IOException {
        return SerializationUtils.loadSchemaRef(
            om, "schemas/" + methodName + ".response.json");
    }

    /** Load schema for Aion RPC type */
    public JsonNode loadSchemaRef(JsonSchemaRef ref)
    throws IOException {
        URL url = RpcSchemaLoader.class.getClassLoader().getResource("schemas/type/" + ref.getFile());
        String schemaTxt = Resources.toString(url, Charsets.UTF_8);
        JsonPointer ptr = JsonPointer.compile(ref.getFragment());
        JsonNode result = om.readTree(schemaTxt).at(ptr);

        if(result.isMissingNode()) {
            throw new SchemaException(String.format(
                    "Could not dereference %s because it led to a non-existent Json node.",
                    ref.getValue()
            ));
        }
        return result;
    }
}
