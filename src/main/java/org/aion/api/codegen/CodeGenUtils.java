package org.aion.api.codegen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;
import org.aion.api.schema.JsonSchemaTypeResolver;
import org.aion.api.schema.NamedRpcType;
import org.aion.api.schema.RootTypes;
import org.aion.api.schema.RpcType;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class CodeGenUtils {
    static Configuration configureFreemarker() {
        Configuration cfg = new Configuration();
        cfg.setClassForTemplateLoading(GenerateRpcProcessor.class, "/templates");
        cfg.setIncompatibleImprovements(new Version(2, 3, 20));
        cfg.setDefaultEncoding("UTF-8");
        cfg.setLocale(Locale.US);
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        return cfg;
    }

    static List<String> loadMethodList() throws IOException {
        URL methodsUrl = Resources.getResource("methods.txt");
        String methods = Resources.toString(methodsUrl, Charsets.UTF_8);
        String[] methodList = methods.split("\n");
        return Arrays.asList(methodList);
    }

    static List<NamedRpcType> retrieveObjectDerivedRpcTypes(ObjectMapper om,
                                                             JsonSchemaTypeResolver resolver)
    throws IOException {
        // as per AionRpc convention, all non-root types live in
        // the resource schemas/type/derived.json.
        URL url = Resources.getResource("schemas/type/derived.json");
        JsonNode derivedTypesRoot = om.readTree(url);
        JsonNode defs = derivedTypesRoot.get("definitions");

        List<NamedRpcType> objectDerived = new LinkedList<>();
        for (Iterator<Map.Entry<String,JsonNode>> it = defs.fields(); it.hasNext(); ) {
            Map.Entry<String,JsonNode> entry = it.next();
            String name = entry.getKey();
            JsonNode def = entry.getValue();

            RpcType type = resolver.resolveSchema(def, name);

            if(type.getRootType().equals(RootTypes.OBJECT)) {
                objectDerived.add(new NamedRpcType(name, type));
            }

        }

        return objectDerived;
    }
}
