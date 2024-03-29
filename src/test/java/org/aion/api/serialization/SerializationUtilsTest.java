package org.aion.api.serialization;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

public class SerializationUtilsTest {

    @Test
    public void loadSchema() throws Exception {
        ObjectMapper om = new ObjectMapper();
        JsonNode node = SerializationUtils.loadSchemaRef(om, "schemas/type/root.json");
        assertThat(node, is(notNullValue()));
    }
}