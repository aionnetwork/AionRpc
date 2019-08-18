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

    public GenerateRpcInterface() { }

    public void generateRpcInterface() throws Exception {
        Configuration freemarker = CodeGenUtils.configureFreemarker();

        List<String> methods = CodeGenUtils.loadMethodList();
        List<JavaInterfaceMethodDeclaration> declarations = new LinkedList<>();

        for(String method: methods) {
            URL reqUrl = Resources.getResource("schemas/" + method + ".request.json");
            URL rezUrl = Resources.getResource("schemas/" + method + ".response.json");
            String req = Resources.toString(reqUrl, Charsets.UTF_8);
            String rez = Resources.toString(rezUrl, Charsets.UTF_8);
            JsonNode reqRoot = new ObjectMapper().readTree(req);
            JsonNode rezRoot = new ObjectMapper().readTree(rez);

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

            //TODO: Assuming every parameter always has the same type in the RPC method
//            List<String> arguments = Sets.cartesianProduct(inputTypes).iterator().next();

            RpcType retType = resolver.resolveSchema(rezRoot);
            declarations.add(new JavaInterfaceMethodDeclaration(
                    method, retType.getJavaTypeName(), inputTypes));

            // Ftl setup for Request
        }

        Map<String, Object> ftlMap = new HashMap<>();
        ftlMap.put("javaMethodDeclarations", declarations);

        // Apply Freemarker template; output the result
        System.out.println("// == Rpc.java == ");
        Writer consoleWriter = new OutputStreamWriter(System.out);
        freemarker.getTemplate("Rpc.java.ftl").process(ftlMap, consoleWriter);
    }

}
