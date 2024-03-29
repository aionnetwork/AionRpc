package org.aion.api.schema;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Objects;

public class NamedRpcType extends RpcType {
    private String name;

    public NamedRpcType(
                        String name,
                        JsonNode definition,
                        RpcType baseType,
                        JsonNode constraints,
                        List<Field> containedFields,
                        String javaTypeName) {
        super(
            definition,
            baseType,
            constraints,
            containedFields,
            javaTypeName);
        this.name = name;
    }

    public NamedRpcType(String name,
                        RpcType type) {
        super(
            type.getDefinition(),
            type.getBaseType(),
            type.getConstraints(),
            type.getContainedFields(),
            type.getJavaTypeName()
        );
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NamedRpcType)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        NamedRpcType that = (NamedRpcType) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name);
    }
}
