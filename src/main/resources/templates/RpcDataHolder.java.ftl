package org.aion.api.server.rpc2.autogen.pod;

public class ${javaClassName} {
<#list fields as f>
    private ${f.type.javaTypeName} ${f.name};
</#list>

    public ${javaClassName}(
<#list fields as f>
        ${f.type.javaTypeName} ${f.name}<#if (f_has_next)>,</#if>
</#list>
    ) {
<#list fields as f>
        this.${f.name} = ${f.name};
</#list>
    }

<#list fields as f>
    public ${f.type.javaTypeName} get${f.name?cap_first}() {
        return this.${f.name};
    }

</#list>
}
