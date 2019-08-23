package org.aion.api.codegen;

import freemarker.template.Configuration;
import org.aion.api.schema.JsonSchemaErrorResolver;
import org.aion.api.schema.JsonSchemaFixedMixedArrayResolver;
import org.aion.api.schema.JsonSchemaTypeResolver;
import org.aion.api.schema.RpcType;
import org.aion.api.serialization.MethodDescriptor;
import org.aion.api.serialization.RpcSchemaLoader;

import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

        JsonSchemaErrorResolver errorResolver = new JsonSchemaErrorResolver();
        JsonSchemaTypeResolver resolver = new JsonSchemaTypeResolver();
        JsonSchemaFixedMixedArrayResolver arrayResolver =
                new JsonSchemaFixedMixedArrayResolver();

        for(String method: methods) {
            MethodDescriptor md = new RpcSchemaLoader().loadMethod(method);

            // Parameter types to method signatures
            List<String> inputTypes = arrayResolver
                    .resolve(md.getRequest().get("items"))
                    .stream()
                    .map(t -> t.getJavaTypeName())
                    .collect(Collectors.toList());

            // Check the error codes, if exists
            List<String> throwableNames;
            if(md.getError().size() == 0) {
                throwableNames = List.of();
            } else {
                throwableNames = errorResolver.resolve(md.getError())
                        .stream()
                        .map(e -> e.getError().getName())
                        .collect(Collectors.toList());
            }

            RpcType retType = resolver.resolveSchema(md.getResponse());
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

    public class JavaInterfaceMethodDeclaration {
        private final String methodName;
        private final String returnType;
        private final List<String> args;
        private final List<String> exceptions;

        public JavaInterfaceMethodDeclaration(String methodName,
                                              String returnType,
                                              List<String> args,
                                              List<String> exceptions) {
            this.methodName = methodName;
            this.returnType = returnType;
            this.args = args;
            this.exceptions = exceptions;
        }

        public String getMethodName() {
            return methodName;
        }

        public String getReturnType() {
            return returnType;
        }

        public List<String> getArgs() {
            return args;
        }

        public List<String> getExceptions() {
            return exceptions;
        }
    }
}