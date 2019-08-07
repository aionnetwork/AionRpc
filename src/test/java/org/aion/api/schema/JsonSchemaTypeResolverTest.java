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
        assertThat(result.getJavaTypeNames().size(), is(1));
        assertThat(result.getJavaTypeNames().get(0), is("boolean"));
        assertThat(result.getJavaFieldNames().isEmpty(), is(true));
    }

    @Test
    public void resolveBaseTypeBooleanRef() throws Exception {
        JsonNode schema = om.readTree("{\"$ref\":\"root.json#/definitions/BOOLEAN\"}");
        TypeRegistry refs = new TypeRegistry();
        JsonSchemaTypeResolver unit = new JsonSchemaTypeResolver();

        NamedRpcType result = unit.resolveNamedSchema(schema, refs);
        assertThat(result.getName(), is("BOOLEAN"));
        assertThat(result.getJavaTypeNames().size(), is(1));
        assertThat(result.getJavaTypeNames().get(0), is("boolean"));
        assertThat(result.getJavaFieldNames().isEmpty(), is(true));
    }

    // -- Types rooted in DATA --------------------------------------------------------------------

    @Test
    public void resolveBaseTypeData() throws Exception {
        JsonNode schema = om.readTree("{\"$ref\":\"root.json#/definitions/DATA\"}");
        TypeRegistry refs = new TypeRegistry();
        JsonSchemaTypeResolver unit = new JsonSchemaTypeResolver();

        NamedRpcType result = unit.resolveNamedSchema(schema, refs);
        assertThat(result.getName(), is("DATA"));
        assertThat(result.getJavaTypeNames().size(), is(1));
        assertThat(result.getJavaTypeNames().get(0), is("byte[]"));
        assertThat(result.getJavaFieldNames().isEmpty(), is(true));
    }

    // -- Types rooted in QUANTITY ----------------------------------------------------------------

    @Test
    public void resolveBaseTypeQuantity() throws Exception {
        JsonNode schema = om.readTree("{\"$ref\":\"root.json#/definitions/QUANTITY\"}");
        TypeRegistry refs = new TypeRegistry();
        JsonSchemaTypeResolver unit = new JsonSchemaTypeResolver();

        NamedRpcType result = unit.resolveNamedSchema(schema, refs);
        assertThat(result.getName(), is("QUANTITY"));
        assertThat(result.getJavaTypeNames().size(), is(1));
        assertThat(result.getJavaTypeNames().get(0), is("java.math.BigInteger"));
        assertThat(result.getJavaFieldNames().isEmpty(), is(true));
    }

    @Test
    public void testConstrainedData() throws Exception {
        JsonNode schema = om.readTree("{\"$ref\":\"derived.json#/definitions/DATA32\"}");
        TypeRegistry refs = new TypeRegistry();
        JsonSchemaTypeResolver unit = new JsonSchemaTypeResolver();

        NamedRpcType result = unit.resolveNamedSchema(schema, refs);
        assertThat(result.getName(), is("DATA32"));
        assertThat(result.getJavaTypeNames().size(), is(1));
        assertThat(result.getJavaTypeNames().get(0), is("byte[]"));
        assertThat(result.getJavaFieldNames().isEmpty(), is(true));
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
        RpcType result = unit.resolveSchema(schema, refs);

        assertThat(result.getJavaTypeNames().size(), is(3));
        assertThat(result.getJavaTypeNames().size(), is(result.getJavaFieldNames().size()));

        assertThat(result.getJavaFieldNames().contains("myQuantityField"), is(true));
        assertThat(result.getJavaFieldNames().contains("myDataField"), is(true));
        assertThat(result.getJavaFieldNames().contains("myBooleanField"), is(true));

        // the order in which the parameters appear in result.javaNames and result.javaTypes
        // does not matter, but it does matter that the index of a particular java name
        // is using the same index for the corresponding java type
        Map<String, String> resultMap = new HashMap<>();
        for(int ix = 0; ix < result.getJavaFieldNames().size(); ++ix) {
            resultMap.put(result.getJavaFieldNames().get(ix), result.getJavaTypeNames().get(ix));
        }

        assertThat(resultMap.get("myQuantityField"), is("java.math.BigInteger"));
        assertThat(resultMap.get("myDataField"), is("byte[]"));
        assertThat(resultMap.get("myBooleanField"), is("boolean"));
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
        RpcType result = unit.resolveSchema(schema, refs);

        assertThat(result.getJavaFieldNames().size(), is(2));
        assertThat(result.getJavaFieldNames().contains("firstProp"), is(true));
        assertThat(result.getJavaFieldNames().contains("secondProp"), is(true));
        assertThat(result.getJavaTypeNames().size(), is(2));
        assertThat(result.getJavaTypeNames().get(0), is("boolean"));
        assertThat(result.getJavaTypeNames().get(1), is("boolean"));
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