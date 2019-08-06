package org.aion.api.schema;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;

import org.junit.Test;

public class JsonSchemaTypeResolverTest {
    private final File srcResources = new File("src/main/resources");
    private ObjectMapper om = new ObjectMapper();

    @Test
    public void resolveBaseTypeBooleanShorthand() throws Exception {
        JsonNode schema = om.readTree("{\"type\":\"boolean\"}");
        TypeRegistry refs = new TypeRegistry();
        JsonSchemaTypeResolver unit = new JsonSchemaTypeResolver();

        NamedRpcType result = unit.resolveNamedSchema(schema, refs);
        assertThat(result.getName(), is("Boolean"));
        assertThat(result.getJavaTypeNames().size(), is(1));
        assertThat(result.getJavaTypeNames().get(0), is("boolean"));
        assertThat(result.getJavaFieldNames().isEmpty(), is(true));
    }

    @Test
    public void resolveBaseTypeBooleanRef() throws Exception {
        JsonNode schema = om.readTree("{\"$ref\":\"root.json#/definitions/Boolean\"}");
        TypeRegistry refs = new TypeRegistry();
        JsonSchemaTypeResolver unit = new JsonSchemaTypeResolver();

        NamedRpcType result = unit.resolveNamedSchema(schema, refs);
        assertThat(result.getName(), is("Boolean"));
        assertThat(result.getJavaTypeNames().size(), is(1));
        assertThat(result.getJavaTypeNames().get(0), is("boolean"));
        assertThat(result.getJavaFieldNames().isEmpty(), is(true));
    }

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


    /*
    @Test
    public void resolveObjectWithPropertiesWithManyTypes() throws Exception {
        // if this fails, check that the test is executing with working directory
        // as AION_RPC_ROOT/src/test.  Loading the file this way because loading
        // test resources doesn't seem to work...
        JsonNode schema = om.readTree(new File(
                "src/test/resources/test-schemas/object-with-quantity-data-boolean.json"));
        TypeRegistry refs = new TypeRegistry();


        JsonSchemaTypeResolver unit = new JsonSchemaTypeResolver();
        RpcType result = unit.resolve(schema, refs);

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

        assertThat(resultMap.get("myQuantityField"), is("QUANTITY"));
        assertThat(resultMap.get("myDataField"), is("DATA"));
        assertThat(resultMap.get("myBooleanField"), is("boolean"));
    }
    */


    /*
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

    // -- Javascript built-in containers ----------------------------------------------------------
    @Test
    public void resolveObjectWithTwoPrimitiveProperties() throws Exception {
        TypeReferences refs = new TypeReferences();
        JsonNode schema = om.readTree("{" +
                "\"type\":\"object\", " +
                "\"properties\" : { " +
                "\"firstProp\" : {\"type\":\"boolean\"} , " +
                "\"secondProp\" : {\"type\":\"boolean\"} " +
                "} " +
                "}");
        JsonSchemaTypeResolver unit = new JsonSchemaTypeResolver();
        ParamType result = unit.resolve(schema, refs);

        assertThat(result.kind, is(ParamKind.OBJECT));
        assertThat(result.javaNames.size(), is(2));
        assertThat(result.javaNames.contains("firstProp"), is(true));
        assertThat(result.javaNames.contains("secondProp"), is(true));
        assertThat(result.javaTypes.size(), is(2));
        assertThat(result.javaTypes.get(0), is("boolean"));
        assertThat(result.javaTypes.get(1), is("boolean"));
    }



    @Test(expected = IllegalArgumentException.class)
    public void resolveNestedObject() throws Exception {
        TypeReferences refs = new TypeReferences();
        JsonNode schema = om.readTree("{" +
                "\"type\":\"object\", " +
                "\"properties\" : { " +
                "\"firstProp\" : {\"type\":\"object\"} " +
                "} " +
                "}");
        JsonSchemaTypeResolver unit = new JsonSchemaTypeResolver();
        unit.resolve(schema, refs);
    }

    @Test(expected = IllegalArgumentException.class)
    public void resolveObjectMissingProperties() throws Exception {
        TypeReferences refs = new TypeReferences();
        JsonNode schema = om.readTree("{\"type\":\"object\"}");
        JsonSchemaTypeResolver unit = new JsonSchemaTypeResolver();
        unit.resolve(schema, refs);
    }


    // -- Custom non-container types derived from Javascript primitives ---------------------------
*/
}