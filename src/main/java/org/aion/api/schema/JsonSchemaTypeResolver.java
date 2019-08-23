package org.aion.api.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import org.aion.api.serialization.RpcSchemaLoader;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Resolves types from a JsonSchema to types in Java.  JsonSchema
 * info: {@link }https://json-schema.org/latest/json-schema-core.html}
 *
 * This is specific for use with RPC generator (not a tool for general-
 * purpose JsonSchema processing).  Only parts of JsonSchema spec that
 * were needed at the time are implemented (feel free to extend).
 */
public class JsonSchemaTypeResolver {
    private final RpcSchemaLoader loader;

    /** Constructor */
    public JsonSchemaTypeResolver() {
        this(new RpcSchemaLoader());
    }

    /**
     * Constructor, mainly intended for testing -- allows for changing
     * the behaviour of how method name and type schemas are
     * loaded.
     *
     * @param loader RPC Schema loader (methods and types)
     */
    @VisibleForTesting
    public JsonSchemaTypeResolver(RpcSchemaLoader loader) {
        this.loader = loader;
    }

    /**
     * Create the {@link RpcType} that represents an Aion RPC Type from its
     * canonical JsonSchema definition.
     *
     * @param schema JsonSchema definition of the type
     * @return
     */
    public RpcType resolveSchema(JsonNode schema) {
        return resolveSchema(schema, null);
    }

    public RpcType resolveSchema(JsonNode schema,
                                 @Nullable String javaTypeName) {
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
                    if (javaTypeName == null) {
                        throw new SchemaRestrictionException(
                                "Can't resolve an object without being given the Java type name.  Schema: "
                                + schema.toString());
                    }
                    return resolveObject(javaTypeName, schema);
                case "array": // not supported yet
                case "number": // disallowed
                    throw new SchemaRestrictionException("Not allowed to use type " + type);
                default:
                    throw new UnsupportedOperationException(
                        "Unsupported or disallowed 'type' parameter: " + type);
            }
        } else if (schema.has("$ref")) {
            return resolveRef(schema);
        } else if (schema.has("allOf")) {
            return resolveAllOf(schema);
        }

        // couldn't figure out the type, time to give up and fail
        throw new SchemaException(
            "Expected schema to have 'type', '$ref', or 'allOf' but it " +
                "was missing.  Schema was: " + schema.asText());
    }

    public NamedRpcType resolveNamedSchema(JsonNode schema) {
        // if we recognize given schema as a base type, then return that base type
        NamedRpcType baseType = resolveBaseType(schema);
        if(baseType != null) {
            return baseType;
        }

        return resolveRef(schema);
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
            // we don't check for Object because we don't allow users to
            // use that base type directly
        }

        return null;
    }

    private NamedRpcType resolveRef(JsonNode subschema) {
        // item uses "$ref"
        // we need to dereference it by following the pointer in order
        // to construct the RpcType
        JsonNode refNode = subschema.get("$ref");
        final JsonSchemaRef ref = new JsonSchemaRef(refNode.asText());

        JsonNode definition;
        try {
            definition = loader.loadType(ref);
        } catch (IOException ioe) {
            throw new SchemaException(String.format(
                    "Failed to load schema file '%s' when dereferencing pointer '%s'",
                    ref.getFile(),
                    ref.getValue())
            );
        }

        RpcType resolved = resolveSchema(definition, ref.getTypeName());
        return new NamedRpcType(ref.getTypeName(), resolved);
    }

    private RpcType resolveAllOf(JsonNode schema) {
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

        RpcType baseResolved = resolveSchema(base);

        return new RpcType(
                schema,
                baseResolved,
                constraint,
                List.of(),
                baseResolved.getJavaTypeName()
        );
    }

    private RpcType resolveObject(String javaTypeName,
                                  JsonNode schema) {
        JsonNode props = schema.get("properties");
        List<Field> fields = new LinkedList<>();

        if(! schema.has("properties")) {
            throw new SchemaRestrictionException("Types derived from object must specify properties");
        }

        for(Iterator<Entry<String, JsonNode>> iter = props.fields(); iter.hasNext(); ) {
            Map.Entry<String, JsonNode> prop = iter.next();
            JsonNode propDefinition = prop.getValue();
            fields.add(new Field(prop.getKey(),
                resolveSchema(propDefinition), propDefinition));
        }

        return new RpcType(
            schema,
            RootTypes.OBJECT,
            null,
            fields,
            javaTypeName
        );
    }
}
