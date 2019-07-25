package org.aion.api.codegen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.aion.api.schema.ByteArrayInliner;
import org.aion.api.schema.JsonSchemaRef;
import org.aion.api.schema.JsonSchemaTypeResolver;
import org.aion.api.schema.ParamType;
import org.aion.api.schema.SchemaException;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaClient;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

public class GenerateRpcProcessor {

    public static void main(String[] args) throws Exception {
        GenerateRpcProcessor generateRpcProcessor = new GenerateRpcProcessor();
//        System.out.println("=== RpcProcessor2.java ===");
//        generateRpcProcessor.generateRpcProcessor2();

        System.out.println("=== RpcProcessor2.java ===");
        generateRpcProcessor.generateRpcProcessor2();
    }

    public GenerateRpcProcessor() {

    }

    public void generateRpcProcessor2() throws IOException, TemplateException {
        ObjectMapper mapper = new ObjectMapper();

        // Get JsonSchema stuff
        URL typesUrl = Resources.getResource("schemas/type.json");
        String types = Resources.toString(typesUrl, Charsets.UTF_8);
        JsonNode typesSchemaRoot = mapper.readTree(types);

        Map<String, JsonSchemaRef> visitedRefs = new HashMap<>();
        JsonSchemaTypeResolver resolver = new JsonSchemaTypeResolver();
        ByteArrayInliner byteArrayInliner = new ByteArrayInliner(visitedRefs, typesSchemaRoot);

        Configuration freemarker = configureFreemarker();

        List<String> methods = loadMethodList();
        List<JavaMethodCall> javaMethodCalls = new LinkedList<>();

        Map<String, Object> ftlMap = new HashMap<>();

        for(String method: methods) {
            URL reqUrl = Resources.getResource("schemas/" + method + ".request.json");
            URL rezUrl = Resources.getResource("schemas/" + method + ".response.json");
            String req = Resources.toString(reqUrl, Charsets.UTF_8);
            String rez = Resources.toString(rezUrl, Charsets.UTF_8);
            JsonNode reqRoot = new ObjectMapper().readTree(req);
            JsonNode rezRoot = new ObjectMapper().readTree(rez);

            List<String> paramTypes = resolveParamTypes(
                reqRoot, visitedRefs, resolver, byteArrayInliner);
            ParamType retType = byteArrayInliner.inline(
                resolver.resolve(rezRoot, visitedRefs));

            //TODO Asuming no multi-value types for now.
            javaMethodCalls.add(new JavaMethodCall(paramTypes, retType.javaTypes.get(0), method));
        }



        ftlMap.put("javaMethodCalls", javaMethodCalls);

        Writer consoleWriter = new OutputStreamWriter(System.out);
        freemarker.getTemplate("RpcProcessor2.java.ftl").process(ftlMap, consoleWriter);
    }

    private List<String> resolveParamTypes(JsonNode requestSchema,
                                          Map<String, JsonSchemaRef> refsVisited,
                                          JsonSchemaTypeResolver resolver,
                                          ByteArrayInliner byteArrayInliner) {
        // process each parameter in the param list using the JsonSchemaTypeResolver.
        // the top-level schema for the request itself can't use the resolver though,
        // because of its restriction on arrays.  so, handle the array manually.
        JsonNode items = requestSchema.get("items");
        // need a set of types for each param since overloads are allowed
        List<String> paramTypes = new LinkedList<>();

        if(items == null || ! items.isArray()) {
            throw new SchemaException("items must be an array.");
        }
        for(Iterator<JsonNode> it = items.elements(); it.hasNext(); ) {
            JsonNode param = it.next();
            ParamType t = byteArrayInliner.inline(resolver.resolve(param, refsVisited));
            paramTypes.add(t.javaTypes.get(0)); // TODO Assuming no multi-value types for now.
        }

        return paramTypes;
    }

    private Configuration configureFreemarker() {
        Configuration cfg = new Configuration();
        cfg.setClassForTemplateLoading(GenerateRpcProcessor.class, "/templates");
        cfg.setIncompatibleImprovements(new Version(2, 3, 20));
        cfg.setDefaultEncoding("UTF-8");
        cfg.setLocale(Locale.US);
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        return cfg;
    }

    private List<String> loadMethodList() throws IOException {
        URL methodsUrl = Resources.getResource("methods.txt");
        String methods = Resources.toString(methodsUrl, Charsets.UTF_8);
        String[] methodList = methods.split("\n");
        return Arrays.asList(methodList);
    }

    private Schema loadSchema(URL schemaUrl) throws IOException {
        System.out.println("loadSchema: " + schemaUrl.toString());
        try (InputStream inputStream = schemaUrl.openStream()) {
            JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
            SchemaLoader schemaLoader = SchemaLoader.builder()
                .schemaClient(SchemaClient.classPathAwareClient())
                .schemaJson(rawSchema)
                .resolutionScope("file:///home/sergiu/repos/AionRpc/CodeGen/src/main/resources/schemas/") // setting the default resolution scope
                .build();
            return schemaLoader.load().build(); // wtf
        }
    }

}
