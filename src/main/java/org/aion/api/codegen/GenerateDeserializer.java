package org.aion.api.codegen;

import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.template.Configuration;
import org.aion.api.schema.JsonSchemaTypeResolver;

import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

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
        Configuration freemarker = CodeGenUtils.configureFreemarker();

        Map<String, Object> ftlMap = new HashMap<>();
        ftlMap.put("types", CodeGenUtils.retrieveObjectDerivedRpcTypes(om, resolver));

        // Apply Freemarker template; output the result
        System.out.println("// == TemplatedDeserializer.java == ");
        Writer consoleWriter = new OutputStreamWriter(System.out);
        freemarker.getTemplate("TemplatedDeserializer.java.ftl").process(ftlMap, consoleWriter);

        return 0;
    }
}
