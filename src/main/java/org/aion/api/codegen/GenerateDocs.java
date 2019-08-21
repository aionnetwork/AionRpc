package org.aion.api.codegen;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import freemarker.template.Configuration;
import org.aion.api.schema.*;

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

        List<ErrorUsage> errorUsages = errorResolver.resolve(schema);

        return errorUsages
                .stream()
                .map(e -> new Error(
                        new RpcError(e.getErrorName(),
                            errors.get(e.getErrorName()).getCode(),
                            errors.get(e.getErrorName()).getMessage()
                            ),
                        e.getReason())
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

    private TypeInfo buildReturns(JsonNode schema,
                                  JsonSchemaTypeResolver resolver) {
        NamedRpcType returns = resolver.resolveNamedSchema(schema);

        String description = "";
        JsonNode maybeDescription = schema.get("description");
        if(maybeDescription != null) {
            description = maybeDescription.asText();
        }

        return new TypeInfo(returns, description);
    }

    private List<TypeInfo> buildParameters(JsonNode schema,
                                           JsonSchemaTypeResolver resolver) {
        List<TypeInfo> parameters = new LinkedList<>();
        for(Iterator<JsonNode> it = schema.get("items").elements(); it.hasNext() ; ) {
            JsonNode paramJson = it.next();

            NamedRpcType type = resolver.resolveNamedSchema(paramJson);

            String description = "";
            JsonNode maybeDescription = paramJson.get("description");
            if(maybeDescription != null) {
                description = maybeDescription.asText();
            }

            parameters.add(new TypeInfo(type, description));
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
        private final List<TypeInfo> parameters;
        private final TypeInfo returns;
        private final Example example;
        private final List<Error> errors;

        public Method(String name,
                      String description,
                      List<TypeInfo> parameters,
                      TypeInfo returns,
                      Example example,
                      List<Error> errors) {
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

        public List<TypeInfo> getParameters() {
            return parameters;
        }

        public TypeInfo getReturns() {
            return returns;
        }

        public Example getExample() {
            return example;
        }

        public List<Error> getErrors() {
            return errors;
        }
    }

    public static class ObjectFields {
        private final String fieldName;
        private final TypeInfo typeInfo;

        public ObjectFields(String fieldName, TypeInfo typeInfo) {
            this.fieldName = fieldName;
            this.typeInfo = typeInfo;
        }

        public String getFieldName() {
            return fieldName;
        }

        public TypeInfo getTypeInfo() {
            return typeInfo;
        }
    }

    public static class TypeInfo {
//        private final NamedRpcType type;
        private final String description;
        private final String baseTypeName;
        private final String typeDetail;
        /** the types of fields within this type; only used if this type is Object */
        private final List<ObjectFields> containedFields;
        private final List<TypeInfo> additionalObjectDefinitions;

        private TypeInfo(NamedRpcType type, String description) {
            String rootTypeName = type.getRootType().getName();
            if(FORMATTED_ROOT_TYPE_NAMES.containsKey(rootTypeName)) {
                this.baseTypeName = FORMATTED_ROOT_TYPE_NAMES.get(rootTypeName);
            } else {
                this.baseTypeName = rootTypeName;
            }

            NamedRpcType rootType = type.getRootType();
            String maybeTypeDetail = null;
            if(rootType.equals(RootTypes.DATA)
                || rootType.equals(RootTypes.QUANTITY)) {
                if (type.getConstraints() != null) {
                    JsonNode maybeDescription = type.getConstraints().get("description");
                    if (maybeDescription != null) {
                        maybeTypeDetail = type.getConstraints().get("description")
                                .asText();
                    }
                }
            } else if (type.getRootType().equals(RootTypes.OBJECT)) {
                maybeTypeDetail = type.getName();
            }
            this.typeDetail = maybeTypeDetail;
            this.description = description;

            this.containedFields = new LinkedList<>();
            this.additionalObjectDefinitions = new LinkedList<>();
            for(Field cf : type.getContainedFields()) {
                NamedRpcType rt = (NamedRpcType)cf.getType();
//                additionalObjectDefinitions.addAll(findNestedObjects(rt));

                String fieldDescription = null;
                if(cf.getDefinition().has("description")) {
                    fieldDescription = cf.getDefinition().get("description").asText();
                }
                TypeInfo ti = new TypeInfo(rt, fieldDescription);
                containedFields.add(new ObjectFields(cf.getName(), ti));
            }

        }

        private final Map<String, String> FORMATTED_ROOT_TYPE_NAMES = Map.of(
                "DATA", "DATA",
                "QUANTITY", "QUANTITY",
                "OBJECT", "Object",
                "BOOLEAN", "Boolean"
        );

//        private List<TypeInfo> findNestedObjects(NamedRpcType type) {
//            List<TypeInfo> ret = new LinkedList<>();
//            for(Field cf: type.getContainedFields()) {
//                if(cf.getType().getRootType().equals(RootTypes.OBJECT)) {
//                    ret.add(new TypeInfo((NamedRpcType)cf.getType(), "description TODO"));
//                    ret.addAll(findNestedObjects((NamedRpcType)cf.getType()));
//                } else {
//                    return List.of();
//                }
//            }
//            return ret;
//        }

        public String getDescription() {
            return description;
        }

        /** Formatted string of the base type of the parameter */
        public String getBaseTypeName() {
            return baseTypeName;
        }

        /**
         * Details about the type (constraint if DATA/QUANTITY; empty if BOOLEAN;
         * name of the subtype if OBJECT)
         */
        public String getTypeDetail() {
            return typeDetail;
        }

        public List<ObjectFields> getContainedFields() {
            return containedFields;
        }

        public List<TypeInfo> getAdditionalObjectDefinitions() {
            return additionalObjectDefinitions;
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
        private final RpcError error;
        private final String reason;

        public Error(RpcError error, String reason) {
            this.error = error;
            this.reason = reason;
        }

        public RpcError getError() {
            return error;
        }

        public String getReason() {
            return reason;
        }
    }

}
