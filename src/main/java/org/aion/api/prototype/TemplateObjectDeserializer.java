package org.aion.api.prototype;

import com.fasterxml.jackson.databind.JsonNode;
import org.aion.api.schema.NamedRpcType;
import org.aion.api.schema.SchemaValidationException;
import org.aion.api.schema.TypeRegistry;
import org.aion.api.serialization.RpcTypeDeserializer;

public class TemplateObjectDeserializer extends RpcTypeDeserializer {
    @Override
    public Object deserializeObject(JsonNode value,
                                    NamedRpcType type) throws SchemaValidationException {
        switch(type.getName()) {
            case "SomeStruct":
                return new SomeStruct(
                    (byte[]) super.deserialize(
                        value.get("MyData"),
                        (NamedRpcType) type.getContainedFields().get(0).getType()

                    ),
                    (java.math.BigInteger) super.deserialize(
                        value.get("MyQuantity"),
                        (NamedRpcType) type.getContainedFields().get(1).getType()
                        )
                );
            default:
                throw new UnsupportedOperationException(
                    "Don't know how to handle this kind of object");
        }
    }
}
