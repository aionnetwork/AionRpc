package org.aion.api.schema;

import java.util.Objects;

public class Field {
    private final String name;
    private final RpcType type;

    public Field(String name, RpcType type) {
        this.name = name;
        this.type = type;
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
}
