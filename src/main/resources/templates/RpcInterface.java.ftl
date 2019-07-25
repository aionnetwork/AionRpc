public interface RpcInterface {

<#list sigs as s>
    ${rpcMethodName}Response ${rpcMethodName}(
    <#list s as arg>
        ${arg} var${arg_index}<#if (arg_has_next)>,</#if>
    </#list>
    );

</#list>
}
