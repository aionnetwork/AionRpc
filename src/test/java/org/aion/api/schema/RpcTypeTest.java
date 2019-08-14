package org.aion.api.schema;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.Test;

public class RpcTypeTest {
    private ObjectMapper om = new ObjectMapper();

    @Test
    public void getRootType() {
        RpcType root = new RpcType(
            om.createObjectNode(),
            null,
            om.createObjectNode(),
            List.of(),
            "whatevername"
        );
        RpcType child = new RpcType(
            om.createObjectNode(),
            root,
            om.createObjectNode(),
            List.of(),
            "whatevername"
        );
        RpcType grandchild = new RpcType(
            om.createObjectNode(),
            child,
            om.createObjectNode(),
            List.of(),
            "whatevername"
        );
        RpcType greatgrandchild = new RpcType(
            om.createObjectNode(),
            grandchild,
            om.createObjectNode(),
            List.of(),
            "whatevername"
        );

        assertThat(greatgrandchild.getRootType(), is(root));
    }

    @Test
    public void getRootTypeSelf() {
        RpcType root = new RpcType(
            om.createObjectNode(),
            null,
            om.createObjectNode(),
            List.of(),
            "whatevername"
        );

        assertThat(root.getRootType(), is(root));
    }
}