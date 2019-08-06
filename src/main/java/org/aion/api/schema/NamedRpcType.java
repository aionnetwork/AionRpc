package org.aion.api.schema;

import java.util.List;

public class NamedRpcType extends RpcType {
    private String name;

    public NamedRpcType(JsonSchemaRef definition,
                        RpcType baseType,
                        JsonSchemaRef constraints,
                        List<String> javaTypeNames,
                        List<String> javaFieldNames) {
        super(
            definition,
            baseType,
            constraints,
            javaTypeNames,
            javaFieldNames
        );

        this.name = definition.getTypeName();
    }

    public NamedRpcType(String name,
                        RpcType type) {
        super(
            type.getDefinition(),
            type.getBaseType(),
            type.getConstraints(),
            type.getJavaTypeNames(),
            type.getJavaFieldNames()
        );
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
