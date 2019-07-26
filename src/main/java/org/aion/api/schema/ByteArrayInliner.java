package org.aion.api.schema;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class ByteArrayInliner {
    /**
     * Type name replacements
     */
    public final static Map<String, String> REPLACEMENTS = Map.of(
            "DATA",         "byte[]",
            "QUANTITY",     "java.math.BigInteger"
    );

    private JsonReferences refs;
    private JsonNode types;

    /**
     * Constructor
     *
     * @param loadedRefs type name to references to their definitions
     * @param typeDefinitions definitions
     */
    public ByteArrayInliner(JsonReferences loadedRefs,
                            JsonNode typeDefinitions) {
        this.refs = loadedRefs;
        this.types = typeDefinitions;
    }

    /**
     * Replaces custom generated type names with a simpler type.  When
     * some custom type name "T" uses a $ref and the chain of $refs
     * ends at a type name "R" in {@link #REPLACEMENTS}, use the type name
     * that R maps to instead.
     * <p>
     * Examples:
     * <p>
     * 1. ["DATA"] -> ["byte[]"]
     * 2. ["DATA32"] -> ["byte[]"]
     * 3. ["DATA", "string"] -> ["byte[]", "string"]
     * 4. ["DATA", "DATA', "boolean"] -> ["byte[]", "byte[]", "boolean"]
     * 5. ["QUANTITY", "boolean", "DATA32"] -> ["java.lang.BigNumber", "boolean", "byte[]"]
     */
    public ParamType inline(ParamType type) {
        List<String> inlined = type.javaTypes
                .stream()
                .map(jt -> inline(jt))
                .collect(Collectors.toList());

        return new ParamType(type.kind, inlined, type.javaNames, type.refs);
    }

    /**
     * Dereference the $ref corresponding to typeName, then dereference
     * the $ref of that type name, etc, until either:
     * <p>
     * 1) the end of the chain of references is reached -- return initial typeName
     * 2) or a type name in the {@link #REPLACEMENTS} is reached --  return the replacement
     *
     * @param typeName type name
     * @return name of simpler type, if possible; otherwise, the given type name
     */
    private String inline(String typeName) {
        if (REPLACEMENTS.containsKey(typeName)) {
            return REPLACEMENTS.get(typeName);
        }

        JsonSchemaRef maybeRef = refs.get(typeName);
        if (maybeRef == null) {
            return typeName;
        }
        JsonNode definition = maybeRef.dereference(types);

        // if this one defined in terms of another $ref,
        // check if that one can be simplified
        while (definition.has("$ref")) {
            JsonNode next = definition.get("$ref");
            maybeRef = new JsonSchemaRef(next.asText());

            if (REPLACEMENTS.containsKey((maybeRef.getName()))) {
                return REPLACEMENTS.get(maybeRef.getName());
            }
        }

        return typeName;
    }
}