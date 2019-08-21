package org.aion.api.codegen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class GenerateExceptions {
    private final ObjectMapper om;

    public static void main(String[] args) throws Exception {
        System.exit(new GenerateExceptions().go(args));
    }

    private GenerateExceptions() {
        this.om = new ObjectMapper();
    }

    String subpath = "/modApiServer/src/org/aion/api/server/rpc2/autogen/errors/";

    private int go(String[] args) throws IOException, TemplateException {
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

        for(Map.Entry<String, RpcError> error : CodeGenUtils.retrieveErrorDefinitions(om).entrySet()) {
            String filename = error.getKey() + "RpcException.java";
            if(outputRoot != null) {
                consoleWriter = new OutputStreamWriter(
                        new FileOutputStream(outputRoot.toString() + "/" + filename));
            }

            Map<String, Object> ftlMap = new HashMap<>();
            ftlMap.put("errorName", error.getKey());
            ftlMap.put("message", error.getValue().getMessage());
            ftlMap.put("code", error.getValue().getCode());

            System.out.println("// == " + filename + " == ");

            freemarker.getTemplate("RpcMethodException.java.ftl").process(ftlMap, consoleWriter);
        }

        return 0;
    }
}
