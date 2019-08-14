package org.aion.api.schema;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class JsonSchemaTypeResolverTest {
    private final File srcResources = new File("src/main/resources");
    private ObjectMapper om = new ObjectMapper();

    // -- Types rooted in boolean -----------------------------------------------------------------
    @Test
    public void resolveBaseTypeBooleanShorthand() throws Exception {
        JsonNode schema = om.readTree("{\"type\":\"boolean\"}");
        TypeRegistry refs = new TypeRegistry();
        JsonSchemaTypeResolver unit = new JsonSchemaTypeResolver();

        NamedRpcType result = unit.resolveNamedSchema(schema, refs);
        assertThat(result.getName(), is("BOOLEAN"));
        assertThat(result.getJavaTypeName(), is("boolean"));
        assertThat(result.getContainedFields().isEmpty(), is(true));
    }

    @Test
    public void resolveBaseTypeBooleanRef() throws Exception {
        JsonNode schema = om.readTree("{\"$ref\":\"root.json#/definitions/BOOLEAN\"}");
        TypeRegistry refs = new TypeRegistry();
        JsonSchemaTypeResolver unit = new JsonSchemaTypeResolver();

        NamedRpcType result = unit.resolveNamedSchema(schema, refs);
        assertThat(result.getName(), is("BOOLEAN"));
        assertThat(result.getJavaTypeName(), is("boolean"));
        assertThat(result.getContainedFields().isEmpty(), is(true));
    }

    // -- Types rooted in DATA --------------------------------------------------------------------

    @Test
    public void resolveBaseTypeData() throws Exception {
        JsonNode schema = om.readTree("{\"$ref\":\"root.json#/definitions/DATA\"}");
        TypeRegistry refs = new TypeRegistry();
        JsonSchemaTypeResolver unit = new JsonSchemaTypeResolver();

        NamedRpcType result = unit.resolveNamedSchema(schema, refs);
        assertThat(result.getName(), is("DATA"));
        assertThat(result.getJavaTypeName(), is("byte[]"));
        assertThat(result.getContainedFields().isEmpty(), is(true));
    }

    // -- Types rooted in QUANTITY ----------------------------------------------------------------

    @Test
    public void resolveBaseTypeQuantity() throws Exception {
        JsonNode schema = om.readTree("{\"$ref\":\"root.json#/definitions/QUANTITY\"}");
        TypeRegistry refs = new TypeRegistry();
        JsonSchemaTypeResolver unit = new JsonSchemaTypeResolver();

        NamedRpcType result = unit.resolveNamedSchema(schema, refs);
        assertThat(result.getName(), is("QUANTITY"));
        assertThat(result.getJavaTypeName(), is("java.math.BigInteger"));
        assertThat(result.getContainedFields().isEmpty(), is(true));
    }

    @Test
    public void testConstrainedData() throws Exception {
        JsonNode schema = om.readTree("{\"$ref\":\"derived.json#/definitions/DATA32\"}");
        TypeRegistry refs = new TypeRegistry();
        JsonSchemaTypeResolver unit = new JsonSchemaTypeResolver();

        NamedRpcType result = unit.resolveNamedSchema(schema, refs);
        assertThat(result.getName(), is("DATA32"));
        assertThat(result.getJavaTypeName(), is("byte[]"));
        assertThat(result.getContainedFields().isEmpty(), is(true));
    }

    // -- Types rooted in OBJECT ------------------------------------------------------------------

    @Test
    public void resolveObjectWithPropertiesOfManyTypes() throws Exception {
        // if this fails, check that the test is executing with working directory
        // as AION_RPC_ROOT/src/test.  Loading the file this way because loading
        // test resources doesn't seem to work...
        JsonNode schema = om.readTree(new File(
                "src/test/resources/test-schemas/object-with-quantity-data-boolean.json"));
        TypeRegistry refs = new TypeRegistry();


        JsonSchemaTypeResolver unit = new JsonSchemaTypeResolver();
        RpcType result = unit.resolveSchema(schema, refs, true);

        assertThat(result.getContainedFields().size(), is(3));

        assertThat(result.getContainedFields().get(0).getName(), is("myQuantityField"));
        assertThat(result.getContainedFields().get(1).getName(), is("myDataField"));
        assertThat(result.getContainedFields().get(2).getName(), is("myBooleanField"));

        assertThat(result.getContainedFields().get(0).getType().getJavaTypeName(),
            is("java.math.BigInteger"));
        assertThat(result.getContainedFields().get(1).getType().getJavaTypeName(),
            is("byte[]"));
        assertThat(result.getContainedFields().get(2).getType().getJavaTypeName(),
            is("boolean"));
    }

    @Test
    public void resolveObjectWithTwoPrimitiveProperties() throws Exception {
        TypeRegistry refs = new TypeRegistry();
        JsonNode schema = om.readTree("{" +
            "\"type\":\"object\", " +
            "\"properties\" : { " +
            "\"firstProp\" : {\"type\":\"boolean\"} , " +
            "\"secondProp\" : {\"type\":\"boolean\"} " +
            "} " +
            "}");
        JsonSchemaTypeResolver unit = new JsonSchemaTypeResolver();
        RpcType result = unit.resolveSchema(schema, refs, true);

        assertThat(result.getContainedFields().size(), is(2));
        assertThat(result.getContainedFields().get(0).getName(),
            is("firstProp"));
        assertThat(result.getContainedFields().get(1).getName(),
            is("secondProp"));
        assertThat(result.getContainedFields().get(0).getType().getJavaTypeName(),
            is("boolean"));
        assertThat(result.getContainedFields().get(1).getType().getJavaTypeName(),
            is("boolean"));
        assertThat(result.getJavaTypeName(),
            is(JsonSchemaTypeResolver.JAVA_CLASS_NAME_PLACEHOLDER));
    }

    @Test(expected = SchemaRestrictionException.class)
    public void resolveNestedObject() throws Exception {
        TypeRegistry refs = new TypeRegistry();
        JsonNode schema = om.readTree("{" +
            "\"type\":\"object\", " +
            "\"properties\" : { " +
            "\"firstProp\" : {\"type\":\"object\", \"properties\": {\"nestedProp\" : {\"type\":\"boolean\"}}} " +
            "} " +
            "}");
        JsonSchemaTypeResolver unit = new JsonSchemaTypeResolver();
        unit.resolveSchema(schema, refs);
    }

    @Test(expected = SchemaRestrictionException.class)
    public void resolveObjectMissingProperties() throws Exception {
        TypeRegistry refs = new TypeRegistry();
        JsonNode schema = om.readTree("{\"type\":\"object\"}");
        JsonSchemaTypeResolver unit = new JsonSchemaTypeResolver();
        unit.resolveSchema(schema, refs);
    }

    // -- unsupported Javascript built-in scalars ---------------------------------------

    @Test(expected = SchemaRestrictionException.class)
    public void resolveNumber() throws Exception {
        JsonNode schema = om.readTree("{\"type\":\"number\"}");
        TypeRegistry refs = new TypeRegistry();
        JsonSchemaTypeResolver unit = new JsonSchemaTypeResolver();
        unit.resolveSchema(schema, refs);
    }
}