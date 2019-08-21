## 3. API methods

<#list methods as method>
### ${method.name}

${method.description}

#### Parameters

<#if method.parameters?size == 0>
none
<#else>
<#list method.parameters as param>
1. `${param.baseTypeName}`<#if param.typeDetail??> (${param.typeDetail})</#if> - ${param.description}
<#list param.containedFields as f>
- `${f.fieldName}`: `${f.typeInfo.baseTypeName}` <#if f.typeInfo.typeDetail??> (${f.typeInfo.typeDetail})</#if> <#if f.typeInfo.description??>- ${f.typeInfo.description}</#if>
</#list>
</#list>
</#if>

#### Returns

`${method.returns.baseTypeName}`<#if method.returns.typeDetail??> (${method.returns.typeDetail})</#if> - ${method.returns.description}
<#list method.returns.containedFields as f>
- `${f.fieldName}`: `${f.typeInfo.baseTypeName}` <#if f.typeInfo.typeDetail??> (${f.typeInfo.typeDetail})</#if> <#if f.typeInfo.description??>- ${f.typeInfo.description}</#if>
</#list>

#### Errors

<#if method.errors? size == 0>
none
<#else>
<#list method.errors as error>
1. `${error.error.code?c}` (${error.error.message})<#if error.reason??> - ${error.reason}</#if>
</#list>
</#if>
#### Example

<#if method.example??>
##### Request
```
${method.example.request}
```

#### Response
```
${method.example.response}
```

<#else>
none

</#if>

***

</#list>