package org.aion.api.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aion.api.serialization.RpcSchemaLoader;

import java.io.IOException;
import java.util.List;

/**
 * The 'root' types of RPC Autogeneration framework.
 *
 * These correspond to the types in resources/schemas/type/root.json.
 */
public class RootTypes {
    public static final NamedRpcType DATA;
    public static final NamedRpcType QUANTITY;
    public static final NamedRpcType BOOLEAN;
    public static final NamedRpcType OBJECT;

    static {
        final ObjectMapper om = new ObjectMapper();
        final RpcSchemaLoader loader = new RpcSchemaLoader();
        try {
            DATA = new NamedRpcType(
                "DATA",
                loader.loadType(
                        new JsonSchemaRef("root.json#/definitions/DATA")),
                null,
                null,
                List.of(),
                "byte[]"
            );
            QUANTITY = new NamedRpcType(
                "QUANTITY",
                loader.loadType(
                        new JsonSchemaRef("root.json#/definitions/QUANTITY")),
                null,
                null,
                List.of(),
                "java.math.BigInteger"
            );
            BOOLEAN = new NamedRpcType(
                "BOOLEAN",
                loader.loadType(
                        new JsonSchemaRef("root.json#/definitions/BOOLEAN")),
                null,
                null,
                List.of(),
                "boolean"
            );
            OBJECT = new NamedRpcType(
                "OBJECT",
                loader.loadType(
                        new JsonSchemaRef("root.json#/definitions/OBJECT")),
                null,
                null,
                List.of(),
                "java.lang.Object"
            );
        } catch (IOException ioe) {
            throw new ExceptionInInitializerError(ioe);
        }
    }
}
