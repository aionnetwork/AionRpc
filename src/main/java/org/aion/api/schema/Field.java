package org.aion.api.schema;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

public class Field {
    private final String name;
    private final RpcType type;
    private final JsonNode definition;

    public Field(String name, RpcType type, JsonNode definition) {
        this.name = name;
        this.type = type;
        this.definition = definition;
    }

    public String getName() {
        return name;
    }

    public RpcType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Field)) {
            return false;
        }
        Field field = (Field) o;
        return Objects.equals(name, field.name) &&
            Objects.equals(type, field.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }

    public JsonNode getDefinition() {
        return definition;
    }
}
