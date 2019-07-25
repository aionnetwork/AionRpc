package org.aion.api.envelope;

public class JsonRpcResponse {
    private final Object result;
    private final String id;
    private final String jsonrpc;

    public JsonRpcResponse(Object result, String id) {
        this.result = result;
        this.id = id;
        this.jsonrpc = "2.0";
    }

    public Object getResult() {
        return result;
    }

    public String getId() {
        return id;
    }

    public String getJsonrpc() {
        return jsonrpc;
    }
}
