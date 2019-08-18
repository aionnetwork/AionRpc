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
import org.aion.api.schema.JsonSchemaTypeResolver;
import org.aion.api.schema.RpcType;
import org.aion.api.schema.SchemaException;
import org.aion.api.schema.TypeRegistry;

public class GenerateRpcProcessor {

    public static void main(String[] args) throws Exception {
        GenerateRpcProcessor generateRpcProcessor = new GenerateRpcProcessor();
        System.out.println("// === RpcProcessor2.java ===");
        generateRpcProcessor.generateRpcProcessor2();
    }

    public GenerateRpcProcessor() { }

    public void generateRpcProcessor2() throws IOException, TemplateException {
        ObjectMapper mapper = new ObjectMapper();

        // Get JsonSchema stuff
        JsonSchemaTypeResolver resolver = new JsonSchemaTypeResolver();

        Configuration freemarker = CodeGenUtils.configureFreemarker();

        List<String> methods = CodeGenUtils.loadMethodList();
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
                reqRoot, resolver);
            RpcType retType = resolver.resolveSchema(rezRoot);

            //TODO Asuming no multi-value types for now.
            javaMethodCalls.add(new JavaMethodCall(paramTypes, retType.getJavaTypeName(), method));
        }


        ftlMap.put("javaMethodCalls", javaMethodCalls);

        Writer consoleWriter = new OutputStreamWriter(System.out);
        freemarker.getTemplate("RpcProcessor2.java.ftl").process(ftlMap, consoleWriter);
    }

    private List<String> resolveParamTypes(JsonNode requestSchema,
                                          JsonSchemaTypeResolver resolver) {
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
            RpcType t = resolver.resolveSchema(param);
            paramTypes.add(t.getJavaTypeName()); // TODO Assuming no multi-value types for now.
        }

        return paramTypes;
    }

}
