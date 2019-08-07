## Guide
### Where to start?
A good place to start is to look at the existing schemas and how they are used.  If you are adding a new method with parameters and return types similar to an existing method, you can probably deduce what you need by just looking at the examples.  You can play with the schemas and run the code generators to get an intuition of how it works.  For an in-depth explanation of the system, see [Framework](#Framework) section.

##### Repository organization
- Schemas for request and response of RPC methods: https://github.com/aionnetwork/AionRpc/tree/master/src/main/resources/schemas
- Method list for RPC layer: https://github.com/aionnetwork/AionRpc/blob/master/src/main/resources/methods.txt
- User-defined custom types for RPC layer: https://github.com/aionnetwork/AionRpc/blob/master/src/main/resources/schemas/type/derived.json
- Templates for generated Java code: https://github.com/aionnetwork/AionRpc/tree/master/src/main/resources/templates

##### Running the code generators
- To generate Rpc.java: `./gradlew genRpcInterface`
- To generate RpcProcessor.java: `./gradlew genRpcProcessor`
- To generate both files: `./gradlew generateAll`

##### Integration
Integration into the Aion Java kernel is very minimal right now and mostly done manually.  After generating the `Rpc.java` and `RpcProcessor.java` files, you must manually paste them into the kernel.  The file locations are:
- https://github.com/aionnetwork/aion/blob/rpc-autogen/modApiServer/src/org/aion/api/server/rpc2/autogen/Rpc.java
- https://github.com/aionnetwork/aion/blob/rpc-autogen/modApiServer/src/org/aion/api/server/rpc2/autogen/RpcProcessor2.java

Apart from these generated source files, the Aion Java kernel uses classes from this repository to perform serialization/deserialization.  When this project is built via `./gradlew build`, the resulting jar in `build/lib` contains those classes and also the schema files.  It needs to be copied to this location:
- https://github.com/aionnetwork/aion/blob/rpc-autogen/lib/AionRpc.jar

If you wish to understand how these two classes fit into the kernel, a good place to start is to look at how adjacent classes interact with them.
##### How do I...
- If you want to add/edit/delete methods:
  1. First, Modify the methods list  https://github.com/aionnetwork/AionRpc/blob/master/src/main/resources/methods.txt, then
  1. Modify https://github.com/aionnetwork/AionRpc/tree/master/src/main/resources/schemas
  1. Then follow the steps from [Integration](#integration) for these changes to be reflected in the Aion Java kernel.
 - If you want to edit the FreeStyle templates: see https://github.com/aionnetwork/AionRpc/tree/master/src/main/resources/templates

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

| RPC type | JSON representation              | Java representation |
| -------- | -------------------------------- | ------------------- |
| BOOLEAN  | boolean                          | boolean or Boolean  |
| DATA     | string with regex constraint     | byte[]              |
| QUANTITY | string with regex constraint     | BigNumber           |

The JSON representation of an RPC type is its canonical representation.   By convention, all root types are defined in the file https://github.com/aionnetwork/AionRpc/blob/master/src/main/resources/schemas/type/root.json.  

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

Currently, the code generation will output two files, Rpc.java and RpcProcessor2.java.  They are intended to be used within the Aion Java kernel.

##### Rpc.java

This is a Java interface containing all methods of the RPC server.  The method signatures correspond to the request and response schemas of the method in question.  Types that were defined in JsonSchema have a Java representation in this interface.  https://github.com/aionnetwork/aion/blob/rpc-autogen/modApiServer/src/org/aion/api/server/rpc2/RpcImpl.java is the class implementing the interface.  Therefore, after a modification to the schemas or types have been made, a change in the implementing class needs to also be updated.  This interface is responsible for defining the input and output types of the RPC layer.

##### RpcProcessor2.java

This class is close to the entry point of the RPC layer.  It receives the RPC request (serialized to a `JsonRpcRequest` Java object) and invokes the appropriate method in Rpc.java.  Determining the correct type of each argument and response of the requested RPC method is the responsibility of this class.