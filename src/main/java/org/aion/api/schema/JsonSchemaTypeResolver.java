package org.aion.api.schema;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Resolves types from a JsonSchema to types in Java.  JsonSchema
 * info: {@link }https://json-schema.org/latest/json-schema-core.html}
 *
 * This is specific for use with RPC generator (not a tool for general-
 * purpose JsonSchema processing).  Only parts of JsonSchema spec that
 * were needed at the time are implemented (feel free to extend).
 *
 * Main method: {@link #resolve(JsonNode, Map)}
 */
public class JsonSchemaTypeResolver {

    /**
     * Given a JsonSchema that represents a type definition for Aion RPC
     * server, return a {@link ParamType} describing that type, which can
     * be used to generate custom Java classes representing that type.
     *
     * There are specific restrictions around the type definition:
     * - the JsonSchema keywords "anyOf" and "allOf" aren't allowed (no real use case)
     * - if a JsonSchema is a container type or keyword "oneOf" is used, the
     *   subschemas within them can't be another container, but they can be a $ref
     *   to a separate schema that is a container.
     * - For all $ref values used, the 'name,' or last 'part' of the value (i.e. for
     *   "types.json#/DEFINITIONS/TAG" it would be "TAG") can only be used once
     *   within a schema (and all its contained elements)
     *
     * @param schema root of the JsonSchema
     * @param refsVisited output variable: callers should give this an empty map;
     *                    will be populated with custom type names and their
     *                    JsonSchema "$ref" value
     * @return type descriptor of the schema
     * @throws SchemaException if an invalid structure is encountered
     */
    public ParamType resolve(JsonNode schema,
                             Map<String, JsonSchemaRef> refsVisited) {
        if (schema.has("type")) {
            // item uses "type" -- look up what it needs to be
            String type = schema.get("type").asText();
            switch (type) {
                case "string":
                    return new ParamType("java.lang.String");
                case "boolean":
                    return new ParamType("boolean");
                case "object":
                    return resolveObject(schema, refsVisited);
                case "array":
                    return resolveArray(schema, refsVisited);
                case "number": // not supporting number for now, not used anyway
                default:
                    throw new UnsupportedOperationException(
                            "Unsupported or disallowed 'type' parameter: " + type);
            }
        } else if (schema.has("oneOf")) {
            return resolveOneOf(schema, refsVisited);
        } else if (schema.has("$ref")) {
//            if("DATA".equals(schema.get("$ref"))) {
//                return new ParamType("byte[]");
//            }
            return resolveRef(schema, refsVisited);
        } else {
            throw new SchemaException(
                    "Expected schema to have 'type', '$ref', or 'oneOf' but it " +
                            "was missing.  Schema was: " + schema.asText());
        }
    }

    private ParamType resolveOneOf(JsonNode subschema,
                                   Map<String, JsonSchemaRef> refsVisited) {
        // item uses "oneOf" -- need to traverse that
        JsonNode items = subschema.get("oneOf");
        List<String> itemTypes = new LinkedList<>();

        for(Iterator<JsonNode> iter = items.elements(); iter.hasNext(); ) {
            JsonNode node = iter.next();
            ParamType paramType = resolve(node, refsVisited);

            if(paramType.isCollection()) {
                throw new RuntimeException("Object properties must be scalar.  " +
                        "If your object needs to hold another container, hold a $ref " +
                        "instead and define it to have the schema of your container.");
            }
            itemTypes.add(paramType.javaTypes.get(0));
        }
        return new ParamType(
            ParamType.ParamKind.ONE_OF, itemTypes, null, refsVisited);
    }

    private ParamType resolveObject(JsonNode subschema,
                                    Map<String, JsonSchemaRef> refsVisited) {
        JsonNode props = subschema.get("properties");
        List<String> propNames = new LinkedList<>();

        // since we know that we never want object to contain
        // another object unless through $ref, we can represent the type with
        // String instead of another ParamKind
        List<String> propTypes = new LinkedList<>();

        for(Iterator<Map.Entry<String, JsonNode>> iter = props.fields(); iter.hasNext(); ) {
            Map.Entry<String, JsonNode> prop = iter.next();
            ParamType propType = resolve(prop.getValue(), refsVisited);

//            if(propType.isCollection()) {
            if(propType.kind == ParamType.ParamKind.OBJECT) {
                throw new RuntimeException("Object properties must be scalar.  " +
                        "If your object needs to hold another container, hold a $ref " +
                        "instead and define it to have the schema of your container.  " +
                        "Was trying to resolve: " + propType.toString());
            }
            propTypes.add(propType.javaTypes.get(0));
            propNames.add(prop.getKey());
        }

        return new ParamType(
            ParamType.ParamKind.OBJECT, propTypes, propNames, refsVisited);
    }

    private ParamType resolveArray(JsonNode subschema,
                                   Map<String, JsonSchemaRef> refsVisited) {
        JsonNode items = subschema.get("elements");
        List<String> itemTypes = new LinkedList<>();

        if(items == null) {
            itemTypes.add("java.lang.Object[]");
        } else {
            for (Iterator<JsonNode> iter = items.elements(); iter.hasNext(); ) {
                JsonNode node = iter.next();
                ParamType paramType = resolve(node, refsVisited);

                if (paramType.isCollection()) {
                    throw new RuntimeException("Object properties must be scalar.  " +
                            "If your object needs to hold another container, hold a $ref " +
                            "instead and define it to have the schema of your container." +
                            "  Can't add:" + paramType);
                }
                itemTypes.add(paramType.javaNames.get(0));
            }
        }
        return new ParamType(
            ParamType.ParamKind.ARRAY, itemTypes, null, refsVisited);
    }

    private ParamType resolveRef(JsonNode subschema,
                                 Map<String, JsonSchemaRef> refsVisited) {
        // item uses "$ref" -- save the value into refsVisited
        // so it can be dereferenced at a later time
        JsonNode node = subschema.get("$ref");
        final JsonSchemaRef ref = new JsonSchemaRef(node.asText());



//        if("DATA".equals(ref.getName()) // $ref points to DATA
//            || (                        // or points to a schema with $ref to DATA
//                node.has("$ref")
//                    && node.get("$ref").asText().endsWith("DATA")
//        )) {
//            return new ParamType("byte[]");
//        }

        JsonSchemaRef maybeExistingRef = refsVisited.get(ref.getName());
        if (maybeExistingRef == null) {
            refsVisited.put(ref.getName(), ref);
        } else if (! ref.equals(maybeExistingRef)) {
            throw new IllegalArgumentException(String.format(
                    "Can't use java type name '%s' for ref '%s' because it was already used for '%s'.  "
                            + "Each Json Schema type name must be unique, even if it's in different files.",
                    ref.getName(), maybeExistingRef.toString(), ref.toString()));
        } // else: nothing to do -- it's already there

        return new ParamType(ref);
    }
}
