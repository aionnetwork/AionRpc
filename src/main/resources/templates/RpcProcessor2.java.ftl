package org.aion.api.server.rpc2.autogen;
import org.aion.api.server.rpc2.AbstractRpcProcessor;
import org.aion.api.serialization.JsonRpcRequest;
import org.aion.api.server.rpc2.autogen.pod.*;
import org.aion.api.RpcException;

/******************************************************************************
 *
 * AUTO-GENERATED SOURCE FILE.  DO NOT EDIT MANUALLY -- YOUR CHANGES WILL
 * BE WIPED OUT WHEN THIS FILE GETS RE-GENERATED OR UPDATED.
 *
 *****************************************************************************/
public class RpcProcessor2 extends AbstractRpcProcessor {
    private final Rpc rpc;

    public RpcProcessor2(Rpc rpc) {
        this.rpc = rpc;
    }

    public Object execute(JsonRpcRequest req) throws RpcException {
        Object[] params = req.getParams();
        switch(req.getMethod()) {
<#list javaMethodCalls as jmc>
            case "${jmc.methodName}":
                return rpc.${jmc.methodName}(
<#list jmc.inputTypes as paramType>
                    (${paramType}) params[${paramType_index}]<#if (paramType_has_next)>,</#if>
</#list>
                );
</#list>
            default: throw RpcException.methodNotFound(req.getMethod());
        }
    }
}
