package org.aion.api.schema;

import java.util.List;

/**
 * The 'root' types of RPC Autogeneration framework.
 *
 * These correspond to the types in resources/schemas/type/root.json.
 *
 */
public class RootTypes {

    public static final NamedRpcType DATA =  new NamedRpcType(
            new JsonSchemaRef("derived.json#/definitions/DATA"),
            null,
            null,
            List.of(),
            "byte[]"
    );

    public static final NamedRpcType QUANTITY =  new NamedRpcType(
            new JsonSchemaRef("derived.json#/definitions/QUANTITY"),
            null,
            null,
            List.of(),
        "java.math.BigInteger"
    );

    public static final NamedRpcType BOOLEAN =  new NamedRpcType(
        new JsonSchemaRef("derived.json#/definitions/BOOLEAN"),
        null,
        null,
        List.of(),
        "boolean"
    );

    public static final NamedRpcType OBJECT = new NamedRpcType(
        new JsonSchemaRef("derived.json#/definitions/OBJECT"),
        null,
        null,
        List.of(),
        "java.lang.Object"
    );

}
