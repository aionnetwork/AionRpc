package org.aion.api.serialization;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class JsonRpcRequest {
    private final String jsonrpc;
    private final String method;
    @JsonIgnore
    private Object[] params;
    private final String id;


    @JsonCreator
    public JsonRpcRequest(@JsonProperty("method") String method,
                          @JsonProperty("id") String id,
                          @JsonProperty("jsonrpc") String jsonrpc) {
        this.jsonrpc = jsonrpc;
        this.method = method;
        this.id = id;
    }

    public String getJsonrpc() {
        return jsonrpc;
    }

    public String getMethod() {
        return method;
    }

    public Object[] getParams() {
        return params;
    }

    public void setParams(Object[] params) {
        this.params = params;
    }

    public String getId() {
        return id;
    }

//    public void test() throws Exception {
//        JsonFactory fact = new JsonFactory();
//        JsonParser p = fact.createParser("[\"0x11\", false, {\"some\" : \"thing\"} ]");
//        while(!p.isClosed()){
//            JsonToken jsonToken = p.nextToken();
//
//            System.out.println("jsonToken = " + jsonToken);
//        }
//    }
}
