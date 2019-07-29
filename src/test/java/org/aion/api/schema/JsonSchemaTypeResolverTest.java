package org.aion.api.schema;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import org.aion.api.schema.ParamType.ParamKind;
import org.junit.Test;

public class JsonSchemaTypeResolverTest {
    private final File srcResources = new File("src/main/resources");
    private ObjectMapper om = new ObjectMapper();

    // -- supported Javascript built-in scalars -----------------------------------------
    @Test
    public void resolveString() throws Exception {
        JsonNode schema = om.readTree("{\"type\":\"string\"}");
        TypeReferences refs = new TypeReferences();
        JsonSchemaTypeResolver unit = new JsonSchemaTypeResolver();

        ParamType result = unit.resolve(schema, refs);
        assertThat(result.javaTypes.size(), is(1));
        assertThat(result.javaTypes.iterator().next(), is("java.lang.String"));
        assertThat(result.isRef(), is(false));
        assertThat(result.isCollection(), is(false));
        assertThat(result.kind, is(ParamKind.SCALAR));
    }

    @Test
    public void resolveBoolean() throws Exception {
        JsonNode schema = om.readTree("{\"type\":\"boolean\"}");
        TypeReferences refs = new TypeReferences();
        JsonSchemaTypeResolver unit = new JsonSchemaTypeResolver();

        ParamType result = unit.resolve(schema, refs);
        assertThat(result.javaTypes.size(), is(1));
        assertThat(result.javaTypes.iterator().next(), is("boolean"));
        assertThat(result.isRef(), is(false));
        assertThat(result.isCollection(), is(false));
        assertThat(result.kind, is(ParamKind.SCALAR));
    }

    // -- unsupported Javascript built-in scalars ---------------------------------------
    @Test(expected = UnsupportedOperationException.class)
    public void resolveNumber() throws Exception {
        JsonNode schema = om.readTree("{\"type\":\"number\"}");
        TypeReferences refs = new TypeReferences();
        JsonSchemaTypeResolver unit = new JsonSchemaTypeResolver();
        unit.resolve(schema, refs);
    }

    // -- Json-schema keywords ----------------------------------------------------------
    @Test
    public void resolveRef() throws Exception {
        TypeReferences refs = new TypeReferences();
        JsonNode schema = om.readTree("{\"$ref\":\"types.json#/definitions/DATA\"}");
        JsonSchemaTypeResolver unit = new JsonSchemaTypeResolver();
        ParamType result = unit.resolve(schema, refs);

        assertThat(result.javaTypes.size(), is(1));
        assertThat(result.javaTypes.iterator().next(), is("DATA"));
        assertThat(result.isRef(), is(true));
        assertThat(result.isCollection(), is(false));
        assertThat(result.kind, is(ParamKind.SCALAR));
        assertThat(result.refs.size(), is(1));
        assertThat(result.refs.get("DATA").getTypeName(), is("DATA"));
        assertThat(result.refs.get("DATA").getValue(), is("types.json#/definitions/DATA"));

        // is refs output even needed?
        assertThat(refs.size(), is(1));
        assertThat(refs.get("DATA"), is(result.refs.get("DATA")));
    }

    //TODO
//    @Test
//    public void resolveAnyOf() throws Exception {
//    }
}