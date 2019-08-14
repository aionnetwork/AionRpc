package org.aion.api.prototype;

import com.fasterxml.jackson.databind.JsonNode;
import org.aion.api.schema.NamedRpcType;
import org.aion.api.schema.RpcType;
import org.aion.api.schema.TypeRegistry;
import org.aion.api.serialization.RpcTypeDeserializer;

public class TemplateObjectDeserializer extends RpcTypeDeserializer {
    public Object deserialize(JsonNode value,
                              NamedRpcType type,
                              TypeRegistry tr) throws Exception {

        switch(type.getName()) {
            case "SomeStruct":
                return new SomeStruct(
                    (byte[]) super.deserialize(
                        value.get("MyData"),
                        type.getContainedFields().get(0).getType().getDefinition(),
                        null
                    ),
                    (java.math.BigInteger) super.deserialize(
                        value.get("MyData"),
                        type.getContainedFields().get(1).getType().getDefinition(),
                        null)
                );
            default:
                throw new UnsupportedOperationException(
                    "Don't know how to handle this kind of object");
        }

//        return null;
    }

}
