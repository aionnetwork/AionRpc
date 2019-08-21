package org.aion.api.codegen;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import freemarker.template.Configuration;
import org.aion.api.schema.JsonSchemaErrorResolver;
import org.aion.api.schema.JsonSchemaTypeResolver;
import org.aion.api.schema.NamedRpcType;
import org.aion.api.schema.RpcError;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class GenerateDocs {
    public static void main(String[] args) throws Exception {
        System.exit(new GenerateDocs().go(args));
    }

    private final ObjectMapper om;

    private GenerateDocs() {
        this.om = new ObjectMapper();
    }

    private int go(String args[]) throws Exception {
        Configuration freemarker = CodeGenUtils.configureFreemarker();
        JsonSchemaTypeResolver resolver = new JsonSchemaTypeResolver();
        JsonSchemaErrorResolver errorResolver = new JsonSchemaErrorResolver();
        Map<String, RpcError> errors = CodeGenUtils.retrieveErrorDefinitions(om);

        List<Method> methods = new LinkedList<>();
        for(Map.Entry<String, String> md : getMethodsAndDescriptions().entrySet()) {
            String method = md.getKey();
            String description = md.getValue();

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

            Method m = new Method(
                    method,
                    description,
                    buildParameters(reqRoot, resolver),
                    buildReturns(rezRoot, resolver),
                    buildExample(reqRoot, rezRoot),
                    buildErrors(errRoot, errorResolver, errors)
            );
            methods.add(m);
        }

        Map<String, Object> ftlMap = new HashMap<>();
        ftlMap.put("methods", methods);

        // Apply Freemarker template; output the result
        Writer consoleWriter = new OutputStreamWriter(System.out);
        freemarker.getTemplate("docs/doc.md.ftl").process(ftlMap, consoleWriter);

        return 0;
    }

    private List<Error> buildErrors(JsonNode schema,
                                    JsonSchemaErrorResolver errorResolver,
                                    Map<String, RpcError> errors) {
        if(schema == null) {
            return List.of();
        }

        List<String> errorNames = errorResolver.resolve(schema);

        return errorNames
                .stream()
                .map(e -> new Error(e,
                        errors.get(e).getMessage(),
                        errors.get(e).getCode())
                ).collect(Collectors.toList());
    }

    private Example buildExample(JsonNode reqSchema,
                                 JsonNode rezSchema)
    throws JsonProcessingException {
        JsonNode reqEx = reqSchema.get("examples");
        JsonNode rezEx = rezSchema.get("examples");

        if(reqEx != null && rezEx != null
            && reqEx.get(0) != null && rezEx.get(0) != null) {

            String reqJson = om.writerWithDefaultPrettyPrinter().writeValueAsString(reqEx.get(0));
            String rezJson = om.writerWithDefaultPrettyPrinter().writeValueAsString(rezEx.get(0));
            return new Example(reqJson, rezJson);
        } else {
            return null;
        }
    }

    private Parameter buildReturns(JsonNode schema,
                                 JsonSchemaTypeResolver resolver) {
        NamedRpcType returns = resolver.resolveNamedSchema(schema);

        String description = "";
        JsonNode maybeDescription = schema.get("description");
        if(maybeDescription != null) {
            description = maybeDescription.asText();
        }

        return new Parameter(returns, description);
    }

    private List<Parameter> buildParameters(JsonNode schema,
                                            JsonSchemaTypeResolver resolver) {
        List<Parameter> parameters = new LinkedList<>();
        for(Iterator<JsonNode> it = schema.get("items").elements(); it.hasNext() ; ) {
            JsonNode paramJson = it.next();

            NamedRpcType type = resolver.resolveNamedSchema(paramJson);

            String description = "";
            JsonNode maybeDescription = paramJson.get("description");
            if(maybeDescription != null) {
                description = maybeDescription.asText();
            }

            parameters.add(new Parameter(type, description));
        }
        return parameters;
    }

    private Map<String, String> getMethodsAndDescriptions() throws IOException {
        URL methodsUrl = Resources.getResource("methods.txt");
        String methods = Resources.toString(methodsUrl, Charsets.UTF_8);
        String[] methodList = methods.split("\n");
        return Arrays.asList(methodList)
                .stream()
                .map(line -> splitMethodLine(line))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map.Entry<String, String> splitMethodLine(String line) {
        String[] pieces = line.split(" ");
        if(pieces.length == 1) {
            return new AbstractMap.SimpleEntry<>(pieces[0], "");
        } else {
            String method = pieces[0];
            String description = line.replaceFirst(method + " ", "");
            return new AbstractMap.SimpleEntry<>(method, description);
        }
    }

    public static class Method {
        private final String name;
        private final String description;
        private final List<Parameter> parameters;
        private final Parameter returns;
        private final Example example;
        private final List<Error> errors;

        public Method(String name,
                      String description,
                      List<Parameter> parameters,
                      Parameter returns,
                      Example example,
                      List<Error> errors
        ) {
            this.name = name;
            this.description = description;
            this.parameters = parameters;
            this.returns = returns;
            this.example = example;
            this.errors = errors;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public List<Parameter> getParameters() {
            return parameters;
        }

        public Parameter getReturns() {
            return returns;
        }

        public Example getExample() {
            return example;
        }

        public List<Error> getErrors() {
            return errors;
        }
    }

    public static class Parameter {
//        private final NamedRpcType type;
        private final String description;
        private final String baseTypeName;
        private final String typeDetail;

        private Parameter(NamedRpcType type, String description) {
            this.baseTypeName = type.getRootType().getName();

            String maybeTypeDetail = null;
            if(type.getConstraints() != null) {
                JsonNode maybeDescription = type.getConstraints().get("description");
                if(maybeDescription != null) {
                    maybeTypeDetail = type.getConstraints().get("description").asText();
                }
            }
            this.typeDetail = maybeTypeDetail;
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        public String getBaseTypeName() {
            return baseTypeName;
        }

        public String getTypeDetail() {
            return typeDetail;
        }
    }

    public static class Returns {
        private final NamedRpcType type;
        private final String description;

        public Returns(NamedRpcType type, String description) {
            this.type = type;
            this.description = description;
        }

        public NamedRpcType getType() {
            return type;
        }

        public String getDescription() {
            return description;
        }
    }

    public static class Example {
        private final String request;
        private final String response;

        public Example(String request, String response) {
            this.request = request;
            this.response = response;
        }

        public String getRequest() {
            return request;
        }

        public String getResponse() {
            return response;
        }
    }

    public static class Error {
        private final String name;
        private final String message;
        private final int code;

        public Error(String name, String message, int code) {
            this.name = name;
            this.message = message;
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public String getMessage() {
            return message;
        }

        public int getCode() {
            return code;
        }
    }
}
