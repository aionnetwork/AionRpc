package org.aion.api.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

public class ParamArrayParser {
    private final ObjectMapper om = new ObjectMapper();

    private final Map<String, Schema> schemas = new HashMap<>();


    public ParamArrayParser() throws Exception{
        loadDefinitions();
    }

    private boolean tryValidate(Schema s, Object p) {
        try {
            s.validate(p);
            return true;
        } catch (ValidationException vx) {
        }
        return false;
    }

    /** an array of type names */
    public List<String> parse(String paramArrayJson) throws IOException {
        JsonNode paramArray = om.readTree(paramArrayJson);

        List<String> ret = new LinkedList<>();
        for(Iterator<JsonNode> it = paramArray.elements(); it.hasNext(); ) {
            JsonNode param = it.next();

            if(param.isTextual()) {
                // the only strings used in our schema are for DATA, QUANTITY, and types
                // derived from DATA. Just handle DATA and QUANTITY for now.
                String p = param.asText();

                if(tryValidate(schemas.get("DATA"), p)) {
                    ret.add("DATA");
                } else if(tryValidate(schemas.get("QUANTITY"), p)) {
                    ret.add("QUANTITY");
                }
                // TODO: support DATA32, ... DATA<N>
            } else if (param.isObject()) {

                for(Map.Entry<String, Schema> schemaEntry : schemas.entrySet()) {
                    if(tryValidate(schemaEntry.getValue(), param)) {
                        ret.add(schemaEntry.getKey());
                        break;
                    }
                }
            } else if (param.isNumber()) {
                throw new RuntimeException("number is not allowed");
            } else if (param.isArray()) {
                throw new RuntimeException("array is not allowed");
            } else {
                throw new RuntimeException("unknown type");

            }
        }
        return ret;
    }

    private void loadDefinitions() throws Exception {
        URL typesUrl = Resources.getResource("schemas/type/root.json");

        try (InputStream inputStream = typesUrl.openStream()) {
            JSONObject typesRoot = new JSONObject(new JSONTokener(inputStream));
            JSONObject defs  = typesRoot.getJSONObject("definitions");
            for(Iterator<String> it = defs.keys(); it.hasNext(); ) {
                String typeName = it.next();
                JSONObject jo = defs.getJSONObject(typeName);
                this.schemas.put(typeName, SchemaLoader.load(jo));
            }
        }
    }
}
