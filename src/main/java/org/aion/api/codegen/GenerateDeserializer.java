package org.aion.api.codegen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.aion.api.schema.JsonSchemaFixedMixedArrayResolver;
import org.aion.api.schema.JsonSchemaTypeResolver;
import org.aion.api.schema.NamedRpcType;
import org.aion.api.schema.RootTypes;
import org.aion.api.schema.RpcType;
import org.aion.api.schema.TypeRegistry;

public class GenerateDeserializer {
    private final ObjectMapper om;
    private final JsonSchemaTypeResolver resolver;

    public static void main(String[] args) throws Exception {
        System.exit(new GenerateDeserializer().go());
    }

    public GenerateDeserializer() {
        this.om = new ObjectMapper();
        this.resolver = new JsonSchemaTypeResolver();
    }

    public int go() throws Exception {
        Configuration freemarker = configureFreemarker();

        Map<String, Object> ftlMap = new HashMap<>();
        ftlMap.put("types", retrieveObjectDerivedRpcTypes());

        // Apply Freemarker template; output the result
        System.out.println("// == TemplatedDeserializer.java == ");
        Writer consoleWriter = new OutputStreamWriter(System.out);
        freemarker.getTemplate("TemplatedDeserializer.java.ftl").process(ftlMap, consoleWriter);

        return 0;
    }

    private List<NamedRpcType> retrieveObjectDerivedRpcTypes() throws IOException {
        // as per AionRpc convention, all non-root types live in
        // the resource schemas/type/derived.json.
        URL url = Resources.getResource("schemas/type/derived.json");
        JsonNode derivedTypesRoot = om.readTree(url);
        JsonNode defs = derivedTypesRoot.get("definitions");

        List<NamedRpcType> objectDerived = new LinkedList<>();
        for (Iterator<Entry<String,JsonNode>> it = defs.fields(); it.hasNext(); ) {
            Entry<String,JsonNode> entry = it.next();
            String name = entry.getKey();
            JsonNode def = entry.getValue();

            RpcType type = resolver.resolveSchema(def, name);

            if(type.getRootType().equals(RootTypes.OBJECT)) {
                objectDerived.add(new NamedRpcType(name, type));
            }

        }

        return objectDerived;
    }

    private Configuration configureFreemarker() {
        Configuration cfg = new Configuration();
        cfg.setClassForTemplateLoading(GenerateDeserializer.class, "/templates");
        cfg.setIncompatibleImprovements(new Version(2, 3, 20));
        cfg.setDefaultEncoding("UTF-8");
        cfg.setLocale(Locale.US);
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        return cfg;
    }
}
