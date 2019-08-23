package org.aion.api.server.rpc2.autogen;
import org.aion.api.server.rpc2.autogen.pod.*;
import org.aion.api.server.rpc2.autogen.errors.*;

/******************************************************************************
*
* AUTO-GENERATED SOURCE FILE.  DO NOT EDIT MANUALLY -- YOUR CHANGES WILL
* BE WIPED OUT WHEN THIS FILE GETS RE-GENERATED OR UPDATED.
*
*****************************************************************************/
public interface Rpc {

<#list javaMethodDeclarations as decl>
    ${decl.returnType} ${decl.methodName}(
<#list decl.args as arg>
        ${arg} var${arg_index}<#if (arg_has_next)>, </#if>
</#list>    )<#if decl.exceptions?size == 0>;</#if><#list decl.exceptions> throws <#items as ex>${ex}RpcException<#if ex_has_next>, <#else>;</#if></#items>
</#list>
</#list>
}
