## Guide
### Where to start?
A good place to start is to look at the existing schemata and how they are used.  If you are adding a new method with parameters and return types similar to an existing method, you can probably deduce what you need by just looking at the examples.  You can play with the schemata and run the code generators to get an intuition of how it works.  For an in-depth explanation of the system, see [Framework](#Framework) section.

##### Schema organization
- Method list for RPC layer: https://github.com/aionnetwork/AionRpc/blob/master/src/main/resources/methods.txt
- Method request/response/error schemata: https://github.com/aionnetwork/AionRpc/tree/master/src/main/resources/schemas/method
- Types schemata:
  - User-defined types: https://github.com/aionnetwork/AionRpc/blob/master/src/main/resources/schemas/type/derived.json
  - Root (built-in) types: https://github.com/aionnetwork/AionRpc/blob/master/src/main/resources/schemas/type/root.json
- Errors schemata: https://github.com/aionnetwork/AionRpc/blob/master/src/main/resources/schemas/type/errors.json
- FreeMarker templates (for doc/code generation): https://github.com/aionnetwork/AionRpc/tree/master/src/main/resources/templates 

##### Generate code/docs from schemata

- To generate Java code:
  - Generate all files to a specific location: `./gradlew generateJava -PkernelRoot=/path/to/your/aionkernel`
  - You can also omit the -P argument, which will just print the contents of all files to stdout
- To generate RPC documentation (markdown):
  - Generate to a specific location: `./gradlew generateJava -PdocFile=/location/of/your/choice/doc.md`
  - Omit -P to print to stdout

##### Aion Java kernel update workflow
After you've modified the schema files, you need actually update the Aion Java kernel to reflect those changes.  Run the `/gradlew generateJava -PkernelRoot=/path/to/your/aionkernel` to update the source code of your copy of the Aion Java kernel.  

If you have updated any source code in this repository, you should rebuild this code base and copy the jar into your Java kernel.  `./gradlew build` will build the jar into `build/libs/AionRpc.jar`.  Note that dependencies are not built into this jar.

## Framework
The rest of this document explains the Aion RPC Autogeneration framework in detail.  If you're doing something  more complex or need to modify this framework, the following will give you the full explanation of how to write schemas and how Aion's RPC layer is modeled within this framework.
### Scope
The RPC Autogeneration framework is used to define:
1. The list of methods in Aion's RPC interface
1. The input parameter types and return types of each method
1. The structural definition of the types

These 'types' in the specification are conceptually language-agnostic, though they are defined using JsonSchema.  We will call them "RPC types" to distinguish them from language-specific types.  The Autogeneration framework is responsible for knowing how to represent these types in JSON and in any target language for which it is generating code (for now, just Java).  One goal of this framework is that users of it can easily modify these definitions.  The parameters and return types of each RPC method is in turn also defined in JsonSchema, with references to these type definitions.

### RPC Types
There are two kinds of RPC types: root types and derived types.  The framework has hard-coded understanding of the root types.  Derived types are user-defined types that must be based on exactly one base type (the type it is derived from), with allowance for specifying further constraints.  The framework has functionality for parsing their definitions to perform code generation and serialization.  Unless you are modifying the framework itself, you should not modify or add or remove any root types.

There are currently three root types defined by the framework (array and object will be added in the near future):

| RPC type | JSON representation          | Java representation                |
| -------- | ---------------------------- | ---------------------------------- |
| BOOLEAN  | boolean                      | boolean or Boolean                 |
| DATA     | string with regex constraint | byte[]                             |
| QUANTITY | string with regex constraint | BigNumber                          |
| OBJECT   | object                       | none (not allowed to directly use) |

The JSON representation of an RPC type is its canonical representation.   By convention, all root types are defined in the file https://github.com/aionnetwork/AionRpc/blob/master/src/main/resources/schemas/type/root.json.  OBJECT is a special root type -- it cannot be used directly in RPC method request/response parameters; it is intended to be derived into a custom type, which is in turn used in request/response parameters.

##### Derived types

Derived types have one base type and, optionally, an additional set of constraints.  The base type can be either a root type or another derived type.  The way that this is expressed in JsonSchema is using the `allOf` JsonSchema keyword.  Suitable use-cases for them are to define length-constrained versions of root types, or to define container types with a particular structure (not yet supported) that may be used as input or output parameters in the RPC methods.

By convention, derived types are defined in https://github.com/aionnetwork/AionRpc/blob/master/src/main/resources/schemas/type/derived.json, within the `definitions` object.  We will look at one example from that file:

```
{
    "DATA32": {
      "description": "DATA, 32 bytes",
      "allOf": [
        {"$ref": "root.json#/definitions/DATA"},
        {"minLength": 66, "maxLength": 66}
      ]
     } 
}
```

In this case, there is a custom type named DATA32.  Its base type is the root type `DATA`.  The additional constraint is specified by the `{"minLength": 66, "maxLength": 66}` object.  Note that the outermost JSON object as well as the objects in the JSON array are all JsonSchemas.  For more information on JsonSchema keywords for specifying constraints, see https://json-schema.org/latest/json-schema-validation.html#rfc.section.6.  

In the current implementation, the array of `allOf` must contain exactly two elements.

##### Serialization

The framework has hard-coded understanding of how each root RPC type can be converted from JSON to Java and vice versa.  Since derived types are based on exactly one type, any derived type is rooted in a single root type.  The procedure converting a JSON value to/from a Java value for any derived type is equivalent to that of its root type.   During serialization/deserialization for a value, validation on the JSON is performed against a schema, according to the RPC method being called.  Constraints on derived types are checked at this time.

##### JsonSchema restrictions

JsonSchema is intended to be able to represent any kind of Json structure.  While it is powerful in its expessiveness, our data types don't have the complexity that warrant using all of its features.   Therefore, some limitations are imposed in order to make the schema easier to understand and the implementation of the schema interpreters simpler.  The following restrictions are in place by design:

- In all JsonSchemas for types, if the`$ref` keyword is used, it must refer to a root type or derived type (i.e. is defined in root.json or derived.json)
- In all JsonSchemas for types, if the `type` keyword is used, the only valid value is `boolean` (in the framework implementation, this is treated as if you used `{"$ref":"root.json#/definitions/BOOLEAN"}`)
- When defining a JsonSchema whose base type is object, all properties must use a schema that is either `{"type":"boolean"}` or use a `$ref` pointing to a type defined in root.json or derived.json.
- When defining a JsonSchema that uses the `allOf` keyword, there must be exactly two elements in its array: one is a `$ref` to a type defined in root.json or derived.json; the other must not use `type`, `$ref`, or `allOf` and only contain JsonSchema "validation" keywords (see https://json-schema.org/latest/json-schema-validation.html#rfc.section.6).
- The keywords `anyOf` and `oneOf` must not be used

##### JsonSchema $ref keyword

As defined in the [JsonSchema spec](https://json-schema.org/specification.html), a `$ref`'s value is a [Json Pointer](https://tools.ietf.org/html/rfc6901).  Currently, this framework requires the Json Pointer to be in the form: `file.json#/pointer/to/JsonSchema`.  The resolution of the file path before the `#` starts at https://github.com/aionnetwork/AionRpc/tree/master/src/main/resources/schemas/type.

### RPC Methods

Apart from defining Rpc types, this framework also facilitates defining the input and output types of RPC methods.  This is, in fact, the overarching purpose of the framework.

The list of methods is in the file https://github.com/aionnetwork/AionRpc/blob/master/src/main/resources/methods.txt.  Each line is one method name.  For each method name, there must be a corresponding request and response schema inside https://github.com/aionnetwork/AionRpc/tree/master/src/main/resources/schemas using the filename of the form: `<METHOD_NAME>.request.json` and `<METHOD_NAME>.response.json`.  Each JSON file is a JsonSchema.  

For requests, the schema must use `{"type": "array"}` .  In specifying its items, `$ref` should be used to point to types defined in root.json or derived.json.  It is prohibited to inline a JsonSchema into the items list without using `$ref` (i.e. by using `{"type":"object"}` or some other type within the schema for the request.  `{"type":"boolean"}`is allowed as shorthand to `{"$ref": "root.json#/definitions/BOOLEAN"}`.

For response, the schema must be a single `$ref` to the value that is being returned, or `{"type":"boolean"}`.

### Code Generation

##### Rpc.java

This is a Java interface containing all methods of the RPC server.  The method signatures correspond to the request and response schemas of the method in question.  Types that were defined in JsonSchema have a Java representation in this interface.  https://github.com/aionnetwork/aion/blob/rpc-autogen/modApiServer/src/org/aion/api/server/rpc2/RpcImpl.java is the class implementing the interface.  Therefore, after a modification to the schemas or types have been made, a change in the implementing class needs to also be updated.  This interface is responsible for defining the input and output types of the RPC layer.

##### RpcProcessor2.java

This class is close to the entry point of the RPC layer.  It receives the RPC request (serialized to a `JsonRpcRequest` Java object) and invokes the appropriate method in Rpc.java.  Determining the correct type of each argument and response of the requested RPC method is the responsibility of this class.