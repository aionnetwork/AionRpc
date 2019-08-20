package org.aion.api.codegen;

class RpcError {
    private final int code;
    private final String message;

    public RpcError(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
