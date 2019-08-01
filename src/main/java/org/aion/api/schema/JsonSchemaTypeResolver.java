package org.aion.api.schema;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 *  - handle Object
 *
 */
public class JsonSchemaTypeResolver {
    private ObjectMapper om = new ObjectMapper();

    public RpcType resolveSchema(JsonNode schema,
                                 TypeRegistry types) {
        RpcType baseType;

        // if we recognize given schema as a base type, then return that base type
        baseType = resolveBaseType(schema);
        if(baseType != null) {
            return baseType;
        }

        // otherwise, it is some type derived from a base type.
        // need to build up a new RpcType object that represents it.
        if (schema.has("type")) {
            String type = schema.get("type").asText();
            switch (type) {
                case "object": // coming soon
                case "array": // not supported yet
                case "number": // disallowed
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
            "Expected schema to have 'type', '$ref', or 'oneOf' but it " +
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
            return BaseTypes.BOOLEAN;
        }

        schemaTypeNode = schema.get("$ref");
        if(schemaTypeNode != null) {
            JsonSchemaRef ref = new JsonSchemaRef(schemaTypeNode.asText());
            // should check that the pointer actually matches the built-in ones
            if (ref.getTypeName().equals("QUANTITY")) {
                return BaseTypes.QUANTITY;
            } else if (ref.getTypeName().equals("DATA")) {
                return BaseTypes.DATA;
            } else if (ref.getTypeName().equals("Boolean")) {
                return BaseTypes.BOOLEAN;
            }
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
            JsonNode refFileRoot = Utils.loadSchema(om, "schemas/" + ref.getFile());

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
        return new NamedRpcType(ref.getTypeName(), resolved);
    }

    private RpcType resolveAllOf(JsonNode schema,
                                 TypeRegistry types) {
        JsonNode allOf = schema.get("allOf");
        if(allOf.size() > 2) {
            throw new SchemaRestrictionException("allOf must have only two elements");
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

        JsonSchemaRef ref = new JsonSchemaRef(allOf.asText());
        return new RpcType(
                ref,
                new JsonSchemaRef(base.asText()),
                new JsonSchemaRef(constraint.asText()),
                baseResolved.getJavaTypeNames(),
                List.of()
        );
    }
}
