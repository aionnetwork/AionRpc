# RpcGenerator

Generates Java source files for RPC server in the Aion Java kernel.  

This is under development, so pieces are moving around very often.  Its integration is fairly basic for now (you have to copy and paste the outputted source files into the Aion Java kernel manually).

## Repository guide

- If you want to add/edit/delete methods:
  1. Modify the methods list: https://github.com/aionnetwork/AionRpc/blob/master/src/main/resources/methods.txt, then
  1. Modify https://github.com/aionnetwork/AionRpc/tree/master/src/main/resources/schemas.  See [Schemas](#Schemas)
- If you want to change the Java code that gets generated:
  1. Edit the FreeStyle templates: https://github.com/aionnetwork/AionRpc/tree/master/src/main/resources/templates

### What's supported

Right now, param types and return types must be DATA, QUANTITY, or boolean.

### Coming soon

- Using the JsonSchema `anyOf` keyword for typse
- Defining (in the schema) custom non-container types derived from Javascript primitives or DATA or QUANTITY (to, for instance, create a type with length constraints)
- Defining (in the schema) custom object types with specific fields
- Better/more convenient integration of the ouputted code into the kernel
