package org.aion.api.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class JsonSchemaErrorResolverTest {
    @Test
    public void testSingle() throws Exception  {
        JsonSchemaErrorResolver unit = new JsonSchemaErrorResolver();
        JsonNode input = new ObjectMapper().readTree(
                "{\"$ref\": \"errors.json#/definitions/Unauthorized\"}"
        );
        List<String> result = unit.resolve(input).stream().map(e->e.getErrorName()).collect(Collectors.toList());
        assertThat(result.size(), is(1));
        assertThat(result.get(0), is("Unauthorized"));
    }

    @Test
    public void testMultiple() throws Exception {
        JsonSchemaErrorResolver unit = new JsonSchemaErrorResolver();
        JsonNode input = new ObjectMapper().readTree(
        "  {\"anyOf\": [" +
                "    {\"$ref\": \"errors.json#/definitions/Unauthorized\"}," +
                "    {\"$ref\": \"errors.json#/definitions/ImATeapot\"}" +
                "  ]}"
        );
        List<String> result = unit.resolve(input).stream().map(e->e.getErrorName()).collect(Collectors.toList());
        assertThat(result.size(), is(2));
        assertThat(result.contains("Unauthorized"), is(true));
        assertThat(result.contains("ImATeapot"), is(true));
    }
}