package org.aion.api.schema;

public class RpcError {
    private final String name;
    private final int code;
    private final String message;

    public RpcError(String name, int code, String message) {
        this.name = name;
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getName() {
        return name;
    }
}
