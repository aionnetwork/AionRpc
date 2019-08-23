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
 * Responsible for loading JsonSchema files for methods and types
 */
public class RpcSchemaLoader {
    // Implementation note: eventually we should make a subclass of this
    // where the JsonSchema of methods and types are cached in memory, so
    // calling RPC methods in the kernel doesn't require repeatedly loading
    // files.
    //
    // Either an LRU cache or just a straight up map of pre-determined commonly
    // used methods/types will do.  Invalidation of the cache will be trivial
    // because the definitions don't change during the lifetime of the kernel.

    private ObjectMapper om = new ObjectMapper();

    // -- Load method -------------------------------------------------------------------
    public MethodDescriptor loadMethod(String methodName) throws IOException {
        JsonNode method = SerializationUtils.loadSchemaRef(
                om, "schemas/method/" + methodName + ".json");
        JsonNode req = method.get("definitions").get("request");
        JsonNode resp = method.get("definitions").get("response");
        JsonNode err = method.get("definitions").get("errors");
        String description = method.get("description").asText();
        return new MethodDescriptor(methodName, req, resp, err, description);
    }

    public JsonNode loadRequestSchema(String methodName) throws IOException {
        return loadMethod(methodName).getRequest();
    }

    public JsonNode loadResponseSchema(String methodName) throws IOException {
        return loadMethod(methodName).getResponse();
    }

    public JsonNode loadErrorSchema(String methodName) throws IOException {
        return loadMethod(methodName).getError();
    }

    public String loadMethodDescription(String methodName) throws IOException {
        return loadMethod(methodName).getDescription();
    }

    // -- Load types --------------------------------------------------------------------

    /** Load schema for Aion RPC type */
    public JsonNode loadType(JsonSchemaRef ref) throws IOException {
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