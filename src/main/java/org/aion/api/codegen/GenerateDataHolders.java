package org.aion.api.codegen;

import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import org.aion.api.schema.Field;
import org.aion.api.schema.JsonSchemaTypeResolver;
import org.aion.api.schema.NamedRpcType;

import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GenerateDataHolders {
    private final ObjectMapper om;
    private final JsonSchemaTypeResolver resolver;

    public static void main(String[] args) throws Exception {
         System.exit(new GenerateDataHolders().go(args));
    }

    public GenerateDataHolders() {
        this.om = new ObjectMapper();
        this.resolver = new JsonSchemaTypeResolver();
    }

    String subpath = "/modApiServer/src/org/aion/api/server/rpc2/autogen/pod/";

    public int go(String[] args) throws IOException, TemplateException {
        boolean useStdout = args.length != 1;

        File outputRoot = null;
        Writer consoleWriter = null;
        if(useStdout) {
            consoleWriter = new OutputStreamWriter(System.out);
        } else {
            outputRoot = new File(args[0] + subpath);
            if(! Files.exists(outputRoot.toPath())) {
                System.out.println("Directory does not exist, so giving up: " + outputRoot.toString());
            }
        }

        Configuration freemarker = CodeGenUtils.configureFreemarker();

        for(NamedRpcType type : CodeGenUtils.retrieveObjectDerivedRpcTypes(om, resolver)) {
            String filename = type.getName() + ".java";
            if(outputRoot != null) {
                consoleWriter = new OutputStreamWriter(
                    new FileOutputStream(outputRoot.toString() + "/" + filename));
            }

            List<Field> fields = type.getContainedFields();

            Map<String, Object> ftlMap = new HashMap<>();
            ftlMap.put("javaClassName", type.getName());
            ftlMap.put("fields", fields);

            System.out.println("creating " + filename);

            freemarker.getTemplate("RpcDataHolder.java.ftl").process(ftlMap, consoleWriter);
        }

        return 0;
    }

}
