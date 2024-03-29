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
        List<ErrorUsage> result = unit.resolve(input);
        assertThat(result.size(), is(1));
        assertThat(result.get(0).getError().getName(), is("Unauthorized"));
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
        List<ErrorUsage> result = unit.resolve(input);
        List<String> names = result
                .stream()
                .map(e -> e.getError().getName())
                .collect(Collectors.toList());
        assertThat(names.contains("Unauthorized"), is(true));
        assertThat(names.contains("ImATeapot"), is(true));
    }
}
