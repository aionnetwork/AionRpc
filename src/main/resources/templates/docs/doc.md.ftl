## 3. API methods

<#list methods as method>
### ${method.name}

${method.description}

#### Parameters

<#if method.parameters?size == 0>
none
<#else>
<#list method.parameters as param>
1. `${param.type.name}` - ${param.description}
</#list>
</#if>

#### Returns

`${method.returns.type.name}` - ${method.returns.description}

#### Errors

<#if method.errors? size == 0>
none
<#else>
<#list method.errors as error>
1. `${error.code?c}` - ${error.message}
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