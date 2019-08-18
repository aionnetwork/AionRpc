package org.aion.api.serialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;

import org.aion.api.codegen.GenerateDeserializer;
import org.aion.api.schema.*;
import org.junit.Test;

import javax.tools.*;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RequestDeserializerTest {
    private final ObjectMapper om = new ObjectMapper();
    private RpcMethodSchemaLoader schemaLoader = mock(
        RpcMethodSchemaLoader.class);
    private SchemaValidator validator = new SchemaValidator();
    private final JsonNode typesSchemaRoot;

    public RequestDeserializerTest() throws Exception {
        URL typesUrl = Resources.getResource("schemas/type/root.json");
        String types = Resources.toString(typesUrl, Charsets.UTF_8);
        typesSchemaRoot = om.readTree(types);
    }

    @Test
    public void testMixOfRootScalars() throws Exception {
        JsonNode requestSchema = om.readTree(
            "{"
                + "\"type\": \"array\","
                + "\"items\" : "
                + "[ "
                + "{ \"$ref\" : \"root.json#/definitions/DATA\" }, "
                + "{ \"$ref\" : \"root.json#/definitions/QUANTITY\" }, "
                + "{ \"type\" : \"boolean\" } "
                + "]}");
        when(schemaLoader.loadRequestSchema("testMethod"))
            .thenReturn(requestSchema);

        String payload = "{                                                                                                                                                                                                                   \n" +
            "  \"method\": \"testMethod\",\n" +
            "  \"params\": [\"0x10\", \"0xe\", true],\n" +
            "  \"id\": \"1\",\n" +
            "  \"jsonrpc\": \"2.0\"\n" +
            "}";
        RequestDeserializer unit = new RequestDeserializer(
            om,
            typesSchemaRoot,
            schemaLoader,
            validator,
            new RootTypeOnlyDeserializer()
        );
        JsonRpcRequest result = unit.deserialize(payload);

        assertThat(result.getMethod(), is("testMethod"));
        assertThat(result.getJsonrpc(), is("2.0"));
        assertThat(result.getId(), is("1"));
        assertThat(result.getParams().length, is(3));
        assertThat(result.getParams()[0], is(new byte[] { 0x10 }));
        assertThat(result.getParams()[1], is(new BigInteger("e", 16)));
        assertThat(result.getParams()[2], is(true));
    }

    @Test
    public void testPassedLengthConstraint() throws Exception {
        JsonNode requestSchema = om.readTree(
            "{"
                + "\"type\": \"array\","
                + "\"items\" : "
                + "[ "
                + "{ \"$ref\" : \"derived.json#/definitions/DATA32\" } "
                + "]}");
        when(schemaLoader.loadRequestSchema("testMethod"))
            .thenReturn(requestSchema);

        String payload = "{                                                                                                                                                                                                                   \n" +
            "  \"method\": \"testMethod\",\n" +
            "  \"params\": [\"0x123456789a123456789a123456789a123456789a123456789a123456789a12345\"],\n" +
            "  \"id\": \"1\",\n" +
            "  \"jsonrpc\": \"2.0\"\n" +
            "}";
        RequestDeserializer unit = new RequestDeserializer(
            om,
            typesSchemaRoot,
            schemaLoader,
            validator,
            new RootTypeOnlyDeserializer()
         );

        try {
            JsonRpcRequest result = unit.deserialize(payload);
        } catch (SchemaValidationException svx) {

        }
    }

    @Test(expected = SchemaValidationException.class)
    public void testFailedLengthConstraint() throws Exception {
        JsonNode requestSchema = om.readTree(
            "{"
                + "\"type\": \"array\","
                + "\"items\" : "
                + "[ "
                + "{ \"$ref\" : \"derived.json#/definitions/DATA32\" } "
                + "]}");
        when(schemaLoader.loadRequestSchema("testMethod"))
            .thenReturn(requestSchema);

        String payload = "{                                                                                                                                                                                                                   \n" +
            "  \"method\": \"testMethod\",\n" +
            "  \"params\": [\"0x10\"],\n" +
            "  \"id\": \"1\",\n" +
            "  \"jsonrpc\": \"2.0\"\n" +
            "}";
        RequestDeserializer unit = new RequestDeserializer(
            om,
            typesSchemaRoot,
            schemaLoader,
            validator,
            new RootTypeOnlyDeserializer()
        );
        unit.deserialize(payload);
    }

    private static class RootTypeOnlyDeserializer extends RpcTypeDeserializer {

        @Override
        protected Object deserializeObject(JsonNode node,
                                           NamedRpcType expectedTypeSchema) throws SchemaValidationException {
            throw new UnsupportedOperationException();
        }
    }

//    @Test
//    public void testcompile() throws Exception {
//        // pretty damn ridiculous
//
//        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
//        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
//
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        System.setOut(new PrintStream(baos));
//        new GenerateDeserializer().go();
//        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
//
//        StringWriter writer = new StringWriter();
//        PrintWriter out = new PrintWriter(writer);
//        out.println(baos.toString());
//
//
//        JavaFileObject file = new JavaSourceFromString("TemplatedDeserializer", writer.toString());
//        Iterable<? extends JavaFileObject> compilationUnits = Arrays.asList(file);
//        JavaCompiler.CompilationTask task = compiler.getTask(null, null, diagnostics, null, null, compilationUnits);
//
//        boolean success = task.call();
//        if (success) {
//            try {
//                Class.forName("org.aion.api.server.rpc2.autogen.TemplatedDeserializer")
//                        .getDeclaredMethod("deserializeObject", new Class[] {
//                                JsonNode.class, NamedRpcType.class, TypeRegistry.class })
//                        .invoke(null, null, null);
//            } catch (ClassNotFoundException e) {
//                System.err.println("Class not found: " + e);
//            } catch (NoSuchMethodException e) {
//                System.err.println("No such method: " + e);
//            } catch (IllegalAccessException e) {
//                System.err.println("Illegal access: " + e);
//            } catch (InvocationTargetException e) {
//                System.err.println("Invocation target: " + e);
//            }
//        }
//
//    }
//
//    class JavaSourceFromString extends SimpleJavaFileObject {
//        final String code;
//
//        JavaSourceFromString(String name, String code) {
//            super(URI.create("string:///" + name.replace('.','/') + Kind.SOURCE.extension),Kind.SOURCE);
//            this.code = code;
//        }
//
//        @Override
//        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
//            return code;
//        }
//    }
}