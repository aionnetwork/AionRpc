package org.aion.api.serialization;

import org.aion.api.RpcException;

public class JsonRpcError  {
    private final RpcException result;
    private final String id;
    private final String jsonrpc;

    public RpcException getResult() {
        return result;
    }

    public String getId() {
        return id;
    }

    public String getJsonrpc() {
        return jsonrpc;
    }

    public JsonRpcError(RpcException result, String id, String jsonrpc) {
        this.result = result;
        this.id = id;
        this.jsonrpc = jsonrpc;
    }
}
