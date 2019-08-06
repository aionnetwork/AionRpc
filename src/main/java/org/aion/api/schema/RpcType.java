package org.aion.api.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
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
    private final JsonSchemaRef definition;

    private final RpcType baseType;
    private final JsonSchemaRef constraints;

//    private final JsonNode typesDefinitions;

    // Java characterization
    private final List<String> javaFieldNames;
    private final List<String> javaTypeNames;

    public List<String> getJavaFieldNames() {
        return javaFieldNames;
    }

    public List<String> getJavaTypeNames() {
        return javaTypeNames;
    }

    /**
     * Constructor.
     *
     * @param definition
     * @param baseType
     * @param constraints
     * @param javaFieldNames
     * @param javaTypeNames
     */
    public RpcType(JsonSchemaRef definition,
                   RpcType baseType,
                   JsonSchemaRef constraints,
                   List<String> javaTypeNames,
                   List<String> javaFieldNames) {
//        checkRefs(definition, baseTypeSchema, constraints, typesDefin itions);

        this.definition = definition;
        this.baseType = baseType;
        this.constraints = constraints;
        this.javaFieldNames = javaFieldNames;
        this.javaTypeNames = javaTypeNames;
//        this.typesDefinitions = typesDefinitions;
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

    public JsonSchemaRef getDefinition() {
        return definition;
    }

    public RpcType getBaseType() {
        return baseType;
    }

    public JsonSchemaRef getConstraints() {
        return constraints;
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
            Objects.equals(javaFieldNames, rpcType.javaFieldNames) &&
            Objects.equals(javaTypeNames, rpcType.javaTypeNames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(definition, baseType, constraints, javaFieldNames, javaTypeNames);
    }
}
