package org.aion.api.server.rpc2.autogen;
import org.aion.api.server.rpc2.autogen.pod.*;

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
        ${arg} var${arg_index}<#if (arg_has_next)>,</#if>
</#list>
    );

</#list>
}
