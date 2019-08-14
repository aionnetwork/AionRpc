package org.aion.api.schema;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import org.aion.api.serialization.Utils;

import java.io.IOException;
import java.util.List;

/**
 * Resolves types from a JsonSchema to types in Java.  JsonSchema
 * info: {@link }https://json-schema.org/latest/json-schema-core.html}
 *
 * This is specific for use with RPC generator (not a tool for general-
 * purpose JsonSchema processing).  Only parts of JsonSchema spec that
 * were needed at the time are implemented (feel free to extend).
 *
 * TODO:
 *  - TypeRegistry consistency check
 */
public class JsonSchemaTypeResolver {
    private ObjectMapper om = new ObjectMapper();

    @VisibleForTesting
    static final String JAVA_CLASS_NAME_PLACEHOLDER = "<UNKNOWN>";

    /**
     * Create the {@link RpcType} that represents an Aion RPC Type from its
     * canonical JsonSchema definition.
     *
     * @param schema JsonSchema definition of the type
     * @param types The types that have already been resolved.  At completion of this method, any
     *              type names visited will be added to this collection.  Not used for anything meaningful
     *              right now, but should be used in the future to ensure type name consistency
     *              and/or do memoization to avoid constantly re-loading the JsonSchemas.
     * @return
     */
    public RpcType resolveSchema(JsonNode schema,
                                 TypeRegistry types) {
        return resolveSchema(schema, types, false);
    }

    @VisibleForTesting
    RpcType resolveSchema(JsonNode schema,
                                 TypeRegistry types,
                                 boolean alloweUnnamedObjects) {
        // if we recognize given schema as a base type, then return that base type
        RpcType baseTypeResolution = resolveBaseType(schema);
        if(baseTypeResolution != null) {
            return baseTypeResolution;
        }

        // otherwise, it is some type derived from a base type.
        // need to build up a new RpcType object that represents it.
        if (schema.has("type")) {
            String type = schema.get("type").asText();
            switch (type) {
                case "object":
                    RpcType resolved = resolveObject(schema, types);
                    if(! alloweUnnamedObjects &&
                        JAVA_CLASS_NAME_PLACEHOLDER.equals(resolved.getJavaTypeName())) {

                        // This happens if a schema contains a subschema that uses
                        // object type directly.  We should never return a RpcType in
                        // this case because it won't be usable.
                        throw new SchemaRestrictionException(
                            "An OBJECT must not contain any property that use a schema deriving directly from"
                                + "OBJECT.  Instead, create a new custom type that derives OBJECT and use a "
                                + "$ref to that type.");
                    }
                    return resolved;
                case "array": // not supported yet
                case "number": // disallowed
                    throw new SchemaRestrictionException("Not allowed to use type " + type);
                default:
                    throw new UnsupportedOperationException(
                        "Unsupported or disallowed 'type' parameter: " + type);
            }
        } else if (schema.has("$ref")) {
            return resolveRef(schema, types);
        } else if (schema.has("allOf")) {
            return resolveAllOf(schema, types);
        }

        // couldn't figure out the type, time to give up and fail
        throw new SchemaException(
            "Expected schema to have 'type', '$ref', or 'allOf' but it " +
                "was missing.  Schema was: " + schema.asText());
    }

    public NamedRpcType resolveNamedSchema(JsonNode schema,
                                           TypeRegistry types) {
        // if we recognize given schema as a base type, then return that base type
        NamedRpcType baseType = resolveBaseType(schema);
        if(baseType != null) {
            return baseType;
        }

        return resolveRef(schema, types);
    }

    private NamedRpcType resolveBaseType(JsonNode schema) {
        JsonNode schemaTypeNode;

        schemaTypeNode = schema.get("type");
        if(schemaTypeNode != null && schemaTypeNode.asText().equals("boolean")) {
            return RootTypes.BOOLEAN;
        }

        schemaTypeNode = schema.get("$ref");
        if(schemaTypeNode != null) {
            JsonSchemaRef ref = new JsonSchemaRef(schemaTypeNode.asText());
            // should check that the pointer actually matches the built-in ones
            if (ref.getTypeName().equals("QUANTITY")) {
                return RootTypes.QUANTITY;
            } else if (ref.getTypeName().equals("DATA")) {
                return RootTypes.DATA;
            } else if (ref.getTypeName().equals("Boolean")) {
                return RootTypes.BOOLEAN;
            }
            // we dont check for Object because we don't allow users to
            // use that base type directly
        }

        return null;
    }

    private NamedRpcType resolveRef(JsonNode subschema,
                                    TypeRegistry types) {
        // item uses "$ref"
        // we need to dereference it by following the pointer in order
        // to construct the RpcType
        JsonNode refNode = subschema.get("$ref");
        final JsonSchemaRef ref = new JsonSchemaRef(refNode.asText());

        JsonNode definition;
        try {
            JsonNode refFileRoot = Utils.loadSchema(om, "schemas/type/" + ref.getFile());

            JsonPointer fragment = JsonPointer.compile(ref.getFragment());
            definition = refFileRoot.at(fragment);
        } catch (IOException ioe) {
            throw new SchemaException(String.format(
                    "Failed to load schema file '%s' when dereferencing pointer '%s'",
                    ref.getFile(),
                    ref.getValue())
            );
        }

        RpcType resolved = resolveSchema(definition, types);
        if(RootTypes.OBJECT.equals(resolved.getBaseType())) {
            // resolveObject() does not know how to fill in the Java type name and uses a
            // placeholder.  The name is based upon the name of the RpcType, which isn't known
            // until now.  So fill it in on its behalf.
            String name = ref.getTypeName();
            return new NamedRpcType(ref.getTypeName(), new RpcType(
                resolved.getDefinition(),
                resolved.getBaseType(),
                resolved.getConstraints(),
                resolved.getContainedFields(),
                name));
        } else {
            return new NamedRpcType(ref.getTypeName(), resolved);
        }
    }

    private RpcType resolveAllOf(JsonNode schema,
                                 TypeRegistry types) {
        JsonNode allOf = schema.get("allOf");
        if(allOf.size() != 2) {
            throw new SchemaRestrictionException("allOf must have exactly two elements");
        }

        JsonNode first = allOf.get(0);

        JsonNode base;
        JsonNode constraint;
        if(first.has("type") || first.has("$ref")) {
            base = first;
            constraint = allOf.get(1);
        } else {
            base = allOf.get(0);
            constraint = first;
        }

        if(constraint.has("type")
            || constraint.has("$ref")
            || constraint.has("allOf")) {
            throw new SchemaRestrictionException("Only one schema inside allOf may" +
                    " use type, $ref, or allOf");
        }

        RpcType baseResolved = resolveSchema(base, types);

        return new RpcType(
                allOf,
                baseResolved,
                constraint,
                List.of(),
                baseResolved.getJavaTypeName()
        );
    }

    /**
     * @implNote The resolved RpcType does not have a Java Class name at this point.  It is
     * the responsibility of {@link #resolveRef(JsonNode, TypeRegistry)} to define.
     */
    private RpcType resolveObject(JsonNode schema,
                                  TypeRegistry types) {
        JsonNode props = schema.get("properties");
        List<Field> fields = new LinkedList<>();

        if(! schema.has("properties")) {
            throw new SchemaRestrictionException("Types derived from object must specify properties");
        }

        for(Iterator<Entry<String, JsonNode>> iter = props.fields(); iter.hasNext(); ) {
            Map.Entry<String, JsonNode> prop = iter.next();
            JsonNode propDefinition = prop.getValue();
            fields.add(new Field(prop.getKey(),
                resolveSchema(propDefinition, types)));
        }

        return new RpcType(
            schema, // <-- makes no sense, re-think this parameter
            RootTypes.OBJECT,
            null,
            fields,
            JAVA_CLASS_NAME_PLACEHOLDER
        );
    }
}
