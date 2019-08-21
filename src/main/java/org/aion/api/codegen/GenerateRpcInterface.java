package org.aion.api.codegen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;
import org.aion.api.schema.*;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class GenerateRpcInterface {
    public static void main(String[] args) throws Exception {
        new GenerateRpcInterface().generateRpcInterface();
    }

    private GenerateRpcInterface() { }

    private void generateRpcInterface() throws Exception {
        Configuration freemarker = CodeGenUtils.configureFreemarker();

        List<String> methods = CodeGenUtils.loadMethodList();
        List<JavaInterfaceMethodDeclaration> declarations = new LinkedList<>();

        for(String method: methods) {
            URL reqUrl = Resources.getResource("schemas/" + method + ".request.json");
            URL rezUrl = Resources.getResource("schemas/" + method + ".response.json");
            URL errUrl;
            try {
                // because errors not mandatory
                errUrl = Resources.getResource("schemas/" + method + ".error.json");
            } catch (IllegalArgumentException iax) {
                errUrl = null;
            }
            String req = Resources.toString(reqUrl, Charsets.UTF_8);
            String rez = Resources.toString(rezUrl, Charsets.UTF_8);
            String err = errUrl != null ? Resources.toString(errUrl, Charsets.UTF_8) : null;
            JsonNode reqRoot = new ObjectMapper().readTree(req);
            JsonNode rezRoot = new ObjectMapper().readTree(rez);
            JsonNode errRoot = err != null ? new ObjectMapper().readTree(err) : null;

            // Resolve types
            JsonSchemaTypeResolver resolver = new JsonSchemaTypeResolver();
            JsonSchemaFixedMixedArrayResolver arrayResolver =
                    new JsonSchemaFixedMixedArrayResolver();

            // Parameter types to method signatures
            List<String> inputTypes = arrayResolver
                    .resolve(reqRoot.get("items"))
                    .stream()
                    .map(t -> t.getJavaTypeName())
                    .collect(Collectors.toList());

            // Check the error codes, if exists
            // TODO should support multiple errors
            List<String> throwableNames;
            if(errRoot == null) {
                throwableNames = List.of();
            } else {
                throwableNames = new JsonSchemaErrorResolver().resolve(errRoot)
                        .stream()
                        .map(e -> e.getErrorName())
                        .collect(Collectors.toList());
            }

            RpcType retType = resolver.resolveSchema(rezRoot);
            declarations.add(new JavaInterfaceMethodDeclaration(
                    method, retType.getJavaTypeName(), inputTypes, throwableNames
            ));

        }

        Map<String, Object> ftlMap = new HashMap<>();
        ftlMap.put("javaMethodDeclarations", declarations);

        // Apply Freemarker template; output the result
        System.out.println("// == Rpc.java == ");
        Writer consoleWriter = new OutputStreamWriter(System.out);
        freemarker.getTemplate("Rpc.java.ftl").process(ftlMap, consoleWriter);
    }

}
