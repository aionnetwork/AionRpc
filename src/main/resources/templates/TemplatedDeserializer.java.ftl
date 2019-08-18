package org.aion.api.server.rpc2.autogen;

import com.fasterxml.jackson.databind.JsonNode;
import org.aion.api.schema.NamedRpcType;
import org.aion.api.schema.SchemaValidationException;
import org.aion.api.schema.TypeRegistry;
import org.aion.api.serialization.RpcTypeDeserializer;
<#list types as t>
import org.aion.api.server.rpc2.autogen.pod.${t.name};
</#list>

/******************************************************************************
*
* AUTO-GENERATED SOURCE FILE.  DO NOT EDIT MANUALLY -- YOUR CHANGES WILL
* BE WIPED OUT WHEN THIS FILE GETS RE-GENERATED OR UPDATED.
*
*****************************************************************************/
public class TemplatedSerializer extends RpcTypeDeserializer {
    @Override
    public Object deserializeObject(JsonNode value,
                                    NamedRpcType type) throws SchemaValidationException {
        switch(type.getName()) {
<#list types as t>
            case "${t.name}":
                return new ${t.name}(
<#list t.containedFields as f>
                    (${f.type.javaTypeName}) super.deserialize(
                        value.get("${f.name}"),
                        (NamedRpcType) type.getContainedFields().get(${f_index}).getType()
                    )<#if (f_has_next)>,</#if>
</#list>
                );
</#list>
            default:
                throw new UnsupportedOperationException(
                    "Don't know how to handle this kind of object");
        }
    }
}
