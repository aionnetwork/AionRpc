package org.aion.api.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * Represents an Aion Rpc Type.
 *
 * Aion Rpc Types are defined in JsonSchema.  This class can
 * represent all the information in a JsonSchema that the Aion
 * Rpc framework understands/uses.  It also holds additional
 * information necessary for the construction of a Java representation
 * (i.e. a class) that structurally reflects a given schema.
 *
 * The logic for construction of the Java representation of the
 * JsonSchema is not in this class (this class is just a holder for
 * that information); that resides in {@see JsonSchemaTypeResolver}.
 */
public class RpcType {

    // JsonSchema definition
    private final JsonNode definition;

    private final RpcType baseType;
    private final JsonNode constraints;

    private final List<Field> containedFields;

    // Java characterization
    private final String javaTypeName;

    public String getJavaTypeName() {
        return javaTypeName;
    }

    public RpcType(JsonNode definition,
                   RpcType baseType,
                   JsonNode constraints,
                   List<Field> containedFields,
                   String javaTypeName) {
//        checkRefs(definition, baseTypeSchema, constraints, typesDefinitions);

        if(baseType == null || ! RootTypes.OBJECT.equals(baseType.getRootType())) {
            Preconditions.checkArgument(containedFields.isEmpty(),
                "containedFields must be empty unless type is rooted in OBJECT");
        }

        this.definition = definition;
        this.baseType = baseType;
        this.constraints = constraints;
        this.containedFields = new LinkedList<>(containedFields);
        this.javaTypeName = javaTypeName;
    }

    private static void checkRefs(JsonSchemaRef definition,
                                  JsonSchemaRef baseTypeSchema,
                                  JsonSchemaRef constraints,
                                  JsonNode typesDefinitions) {
        if(constraints == null) {
            Preconditions.checkArgument(
                definition.equals(baseTypeSchema),
                "If constraint is null, definition must equal baseTypeSchema");
        } else {
            JsonNode schema = definition.getDefinition(typesDefinitions);
            JsonNode allOf = schema.get("allOf");
            Preconditions.checkNotNull(allOf,
                "baseType and constraint were non-null, but definition does not contain \"anyOf\"");
            Preconditions.checkArgument(allOf.size() == 2,
                "Expected exactly two arguments in allOf");
        }
    }

    public JsonNode getDefinition() {
        return definition;
    }

    public RpcType getBaseType() {
        return baseType;
    }

    public JsonNode getConstraints() {
        return constraints;
    }

    public List<Field> getContainedFields() {
        return containedFields;
    }

    public RpcType getRootType() {
        if(isRootType()) {
            return this;
        }

        RpcType parent = getBaseType();
        while(parent.getBaseType() != null) {
            parent = parent.getBaseType();
        }
        return parent;
    }

    public boolean isRootType() {
        return baseType == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RpcType)) {
            return false;
        }
        RpcType rpcType = (RpcType) o;
        return Objects.equals(definition, rpcType.definition) &&
            Objects.equals(baseType, rpcType.baseType) &&
            Objects.equals(constraints, rpcType.constraints) &&
            Objects.equals(containedFields, rpcType.containedFields) &&
            Objects.equals(javaTypeName, rpcType.javaTypeName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(definition, baseType, constraints, containedFields, javaTypeName);
    }
}
