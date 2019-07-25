public class ${rpcMethodName}Response {
<#list fields as f>
    private ${f[0]} ${f[1]};
</#list>

    public ${rpcMethodName}Response(
<#list fields as f>
        ${f[0]} ${f[1]}<#if (f_has_next)>,</#if>
</#list>
    ) {
<#list fields as f>
        this.${f[1]} = ${f[1]};
</#list>
    }
<#list fields as f>
    public ${f[0]} get${f[1]?cap_first}() {
        return this.${f[1]};
    }
</#list>
}
