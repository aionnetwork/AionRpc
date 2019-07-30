package org.aion.api.serialization;

import org.junit.Test;

import static org.junit.Assert.*;

public class RequestDeserializerTest {

    @Test
    public void test() throws Exception {
        //TODO
        String payload = "{                                                                                                                                                                                                                   \n" +
                "  \"method\": \"submitseed\",\n" +
                "  \"params\": [\"0x10\", \"0xee\"],\n" +
                "  \"id\": \"1\",\n" +
                "  \"jsonrpc\": \"2.0\"\n" +
                "}";

        JsonRpcRequest req = new RequestDeserializer(null).deserialize(payload);

        System.out.println(req.getParams());
    }
}