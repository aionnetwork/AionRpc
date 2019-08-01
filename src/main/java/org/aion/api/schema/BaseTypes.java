package org.aion.api.schema;

import java.util.List;

class BaseTypes {

    static NamedRpcType BOOLEAN =  new NamedRpcType(
            new JsonSchemaRef("type/derived.json#/definitions/Boolean"),
            new JsonSchemaRef("type/derived.json#/definitions/Boolean"),
            null,
            List.of("boolean"),
            List.of()
    );

    static NamedRpcType DATA =  new NamedRpcType(
            new JsonSchemaRef("type/derived.json#/definitions/DATA"),
            new JsonSchemaRef("type/derived.json#/definitions/DATA"),
            null,
            List.of("byte[]"),
            List.of()
    );

    static NamedRpcType QUANTITY =  new NamedRpcType(
            new JsonSchemaRef("type/derived.json#/definitions/QUANTITY"),
            new JsonSchemaRef("type/derived.json#/definitions/QUANTITY"),
            null,
            List.of("java.math.BigInteger"),
            List.of()
    );

}
