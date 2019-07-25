package org.aion.api.schema;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Characterizes type information for a parameter in an RPC method, as defined by
 * a JsonSchema.  Also see {@link JsonSchemaTypeResolver}.
 *
 *
 */
public class ParamType {
    /** Kind of parameter; see {@link ParamType} */
    public final ParamKind kind;

    /** Describes if Param is "Many" or */
    public enum ParamKind {
        // incomplete list; only included ones we need currently

        // scalar - can be used for Java primitives or references/custom type
        SCALAR,
        // representing JavaScript built-in types
        ARRAY,
        OBJECT,
        // representing JsonSchema "combiners"
        ONE_OF
    };

    /**
     * Literal name of the Java class/primitive representing the param type;
     * never null/empty.
     *
     * How to interpret this is based on the {@link #kind}:
     * - if scalar, this contains one element - the name of the class
     * - if array, this contains zero or one element - the name of the class
     *   of the type of elements in array
     * - if object, each element is a name of a class and
     *   each element corresponds to a property in the object
     * - if anyOf, each element is a name of the class corrersponding
     *   to a type in the "anyOf"
     */
    public final List<String> javaTypes;

    /**
     * Used only if {@link #kind} is object.  Element N is the name
     * of the property whose type is element N in {@link #javaTypes}.
     */
    public final List<String> javaNames;

    /**
     * Any Java class mentioned in {@link #javaTypes} has an entry, mapping it to the
     * {@code $ref} value in the JsonSchema.
     */
    public final Map<String, JsonSchemaRef> refs;

    /**
     * Construct a scalar without references
     *
     * @param javaType literal Java primitive/class name
     * */
    public ParamType(String javaType) {
        this(ParamKind.SCALAR,
                List.of(javaType),
                List.of("value"),
                Map.of());
    }

    /**
     * Construct a scalar of a reference
     *
     * @param ref
     */
    public ParamType(JsonSchemaRef ref) {
        this(
                ParamKind.SCALAR,
                List.of(ref.getName()),
                List.of("value"),
                Map.of(ref.getName(), ref)
        );
    }

    /**
     * Constructor
     *
     * @param kind see {@link #kind}
     * @param javaTypes see {@link #javaTypes}
     * @param javaNames see {@link #javaNames}
     * @param refs @see {@link #refs}
     */
    public ParamType(ParamKind kind,
                     List<String> javaTypes,
                     List<String> javaNames,
                     Map<String, JsonSchemaRef> refs) {
        if(javaTypes == null || javaTypes.isEmpty()) {
            throw new RuntimeException("Need at least one value in javaTypes");
        }

        if(javaNames == null && kind == ParamKind.OBJECT) {
            throw new RuntimeException("Object must have java names");
        }

        if(kind == ParamKind.SCALAR &&
                (refs.size() > 1 || javaTypes.size() > 1 )) {
            throw new RuntimeException("A scalar can't have more than one value");
        }

        this.kind = kind;
        this.javaTypes = new LinkedList<>(javaTypes);
        this.javaNames = javaNames == null ? List.of("value") : new LinkedList<>(javaNames);
        this.refs = refs == null ? null : new HashMap<>(refs);

    }

    public boolean isRef() {
        return refs.size() == 1;
    }

    public boolean isCollection() {
        return kind != ParamKind.SCALAR;
    }

    public boolean isMappableToJavaPrimitive() {
        return kind == ParamKind.SCALAR && ! isRef();
    }

    @Override
    public String toString() {
        return "ParamType{" +
                "kind=" + kind +
                ", javaTypes=" + javaTypes +
                ", javaNames=" + javaNames +
                ", refs=" + refs +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParamType paramType = (ParamType) o;
        return kind == paramType.kind &&
                Objects.equals(javaTypes, paramType.javaTypes) &&
                Objects.equals(javaNames, paramType.javaNames) &&
                Objects.equals(refs, paramType.refs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, javaTypes, javaNames, refs);
    }
}
