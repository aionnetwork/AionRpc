package org.aion.api.schema;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

import java.util.List;
import org.junit.Test;

public class RpcTypeTest {

    @Test
    public void getRootType() {
        RpcType root = new RpcType(
            new JsonSchemaRef("whatever.json#/definitions/WHATEVERTYPE"),
            null,
            new JsonSchemaRef("whatever.json#/definitions/WHATEVERCONSTRAINT"),
            List.of("what.ever.type"),
            List.of("whatevername")
        );
        RpcType child = new RpcType(
            new JsonSchemaRef("whatever.json#/definitions/WHATEVERTYPE"),
            root,
            new JsonSchemaRef("whatever.json#/definitions/WHATEVERCONSTRAINT"),
            List.of("what.ever.type"),
            List.of("whatevername")
        );
        RpcType grandchild = new RpcType(
            new JsonSchemaRef("whatever.json#/definitions/WHATEVERTYPE"),
            child,
            new JsonSchemaRef("whatever.json#/definitions/WHATEVERCONSTRAINT"),
            List.of("what.ever.type"),
            List.of("whatevername")
        );
        RpcType greatgrandchild = new RpcType(
            new JsonSchemaRef("whatever.json#/definitions/WHATEVERTYPE"),
            grandchild,
            new JsonSchemaRef("whatever.json#/definitions/WHATEVERCONSTRAINT"),
            List.of("what.ever.type"),
            List.of("whatevername")
        );

        assertThat(greatgrandchild.getRootType(), is(root));
    }

    @Test
    public void getRootTypeSelf() {
        RpcType root = new RpcType(
            new JsonSchemaRef("whatever.json#/definitions/WHATEVERTYPE"),
            null,
            new JsonSchemaRef("whatever.json#/definitions/WHATEVERCONSTRAINT"),
            List.of("what.ever.type"),
            List.of("whatevername")
        );

        assertThat(root.getRootType(), is(root));
    }
}