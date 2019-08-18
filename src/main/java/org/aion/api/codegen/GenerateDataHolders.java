package org.aion.api.codegen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import org.aion.api.schema.Field;
import org.aion.api.schema.JsonSchemaTypeResolver;
import org.aion.api.schema.NamedRpcType;
import org.aion.api.schema.RootTypes;
import org.aion.api.schema.RpcType;
import org.aion.api.schema.TypeRegistry;
import org.everit.json.schema.loader.SchemaLoader;

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

            System.out.println("// == " + filename + " == ");

            freemarker.getTemplate("RpcDataHolder.java.ftl").process(ftlMap, consoleWriter);
        }

        return 0;
    }

}
