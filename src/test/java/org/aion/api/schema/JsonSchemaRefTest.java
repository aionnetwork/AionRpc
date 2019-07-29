package org.aion.api.schema;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeCreator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;

public class JsonSchemaRefTest {

    @Test
    public void getFileNonEmpty() {
        JsonSchemaRef unit = new JsonSchemaRef("file.json#/whatever");
        assertThat(unit.getFile(), is("file.json"));
    }

    @Test
    public void getFileEmpty() {
        JsonSchemaRef unit = new JsonSchemaRef("#/whatever");
        assertThat(unit.getFile(), is(""));
    }

    @Test
    public void getFragment() {
        JsonSchemaRef unit = new JsonSchemaRef("#/some/thing");
        String[] fragment = unit.getFragment();
        assertThat(fragment[0], is("some"));
        assertThat(fragment[1], is("thing"));
    }

    @Test
    public void getTypeName() {
        JsonSchemaRef unit = new JsonSchemaRef("#/definitions/DATA99");
        assertThat(unit.getTypeName(), is("DATA99"));
    }

    @Test
    public void getDefinition() throws Exception {
        ObjectMapper om = new ObjectMapper();

        JsonNode root = om.readTree("{ \"definitions\" : {} }");
        JsonNode someDefinition = om.readTree("{ \"type\" : \"number\" }");
        ((ObjectNode) root.get("definitions")).put("DATA99", someDefinition);

        JsonSchemaRef unit = new JsonSchemaRef("#/definitions/DATA99");
        assertThat(unit.getDefinition(root), is(someDefinition));
    }

    @Test
    public void getValue() {
        JsonSchemaRef unit = new JsonSchemaRef("#/definitions/DATA99");
        assertThat(unit.getValue(), is("#/definitions/DATA99"));
    }
}