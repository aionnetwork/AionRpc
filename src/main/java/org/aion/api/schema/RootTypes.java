package org.aion.api.schema;

import java.util.List;

public class RootTypes {

    public static final NamedRpcType BOOLEAN =  new NamedRpcType(
            new JsonSchemaRef("derived.json#/definitions/Boolean"),
            null,
            null,
            List.of("boolean"),
            List.of()
    );

    public static final NamedRpcType DATA =  new NamedRpcType(
            new JsonSchemaRef("derived.json#/definitions/DATA"),
            null,
            null,
            List.of("byte[]"),
            List.of()
    );

    public static final NamedRpcType QUANTITY =  new NamedRpcType(
            new JsonSchemaRef("derived.json#/definitions/QUANTITY"),
            null,
            null,
            List.of("java.math.BigInteger"),
            List.of()
    );

}
