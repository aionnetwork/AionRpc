package org.aion.api.serialization;

public class JsonRpcResponse {
    private final Object result;
    private final Object error;
    private final String id;
    private final String jsonrpc;

    public enum Kind { SUCCESS, ERROR };

    public JsonRpcResponse(Object result, String id, Kind kind) {
        if(kind == Kind.SUCCESS) {
            this.result = result;
            this.error = null;
        } else if (kind == Kind.ERROR) {
            this.result = null;
            this.error = result;
        } else {
            throw new IllegalArgumentException("Don't know that kind");
        }
        this.id = id;
        this.jsonrpc = "2.0";
    }

    public Object getResult() {
        return result;
    }

    public Object getError() {
        return error;
    }

    public String getId() {
        return id;
    }

    public String getJsonrpc() {
        return jsonrpc;
    }

    public Kind getKind() {
        return error == null ? Kind.SUCCESS : Kind.ERROR;
    }
}
