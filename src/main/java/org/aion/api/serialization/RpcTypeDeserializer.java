package org.aion.api.serialization;

import static org.aion.api.serialization.SerializationUtils.hexStringToByteArray;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigInteger;
import org.aion.api.schema.JsonSchemaTypeResolver;
import org.aion.api.schema.NamedRpcType;
import org.aion.api.schema.RootTypes;
import org.aion.api.schema.RpcType;
import org.aion.api.schema.SchemaValidationException;
import org.aion.api.schema.SchemaValidator;

public abstract class RpcTypeDeserializer {
    private final SchemaValidator validator;
    protected final JsonSchemaTypeResolver resolver;

    public RpcTypeDeserializer() {
        this(new SchemaValidator(), new JsonSchemaTypeResolver());
    }

    public RpcTypeDeserializer(SchemaValidator validator,
                               JsonSchemaTypeResolver resolver) {
        this.validator = validator;
        this.resolver = resolver;
    }

    public Object deserialize(JsonNode node,
                              NamedRpcType type)
    throws SchemaValidationException {
        try {
            if (!validator.validate(type.getDefinition(), node)) {
                throw new SchemaValidationException(
                    String.format("Schema validation error at parameter '%s'", node.toString()));
            }
        } catch (JsonProcessingException jpe) {
            throw new SchemaValidationException(String.format(
                "JSON parse error.  Input json: ", node.toString()), jpe);
        }

//        NamedRpcType rpcType = resolver.resolveNamedSchema(expectedTypeSchema, tr);
        RpcType root = type.getRootType();

        // For everything type except those rooted in Object, the serialization
        // procedure is the same as their root type.  Just the validation part
        // is different, which has already happened.

        if(root.equals(RootTypes.BOOLEAN)) {
            return node.asBoolean();
        } else if(root.equals(RootTypes.DATA)) {
            String nodeVal = node.asText();
            return SerializationUtils.hexStringToByteArray(nodeVal);
        } else if(root.equals(RootTypes.QUANTITY)) {
            String nodeVal = node.asText();
            // need to pad it to even-length so it may be converted to byte[]
            if(nodeVal.length() % 2 != 0) {
                nodeVal = nodeVal.replaceFirst("0x", "0x0");
            }
            return new BigInteger(hexStringToByteArray(nodeVal));
        } else if (root.equals(RootTypes.OBJECT)) {
            return deserializeObject(node, type);
        }

        throw new UnsupportedOperationException("Unsupported type");
    }

    protected abstract Object deserializeObject(JsonNode node,
                                                NamedRpcType expectedTypeSchema)
    throws SchemaValidationException;
}