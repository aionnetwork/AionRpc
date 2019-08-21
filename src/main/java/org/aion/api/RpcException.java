package org.aion.api;

import java.util.Optional;

/**
 * Represents an exception in the RPC server.  It can be represented as a
 * JSONRPC 2.0 error object.  See: https://www.jsonrpc.org/specification#error_object.
 */
public class RpcException extends Exception {
    private final int code;
    private final String data;

    protected RpcException(int code, String message) {
        super(message);
        this.code = code;
        this.data = null;
    }

    protected RpcException(int code, String message, String data) {
        super(message);
        this.code = code;
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public Optional<String> getData() {
        return Optional.ofNullable(data);
    }

    public static RpcException parseError(String data) {
        return new RpcException(-32700, "Parse error", data);
    }

    public static RpcException invalidRequest(String data) {
        return new RpcException(-32600, "Invalid request", data);
    }

    public static RpcException methodNotFound(String methodName) {
        return new RpcException(-32601, "Invalid request", methodName);
    }

    public static RpcException invalidParams(String data) {
        return new RpcException(-32602, "Invalid params", data);
    }

    public static RpcException internalError(String data) {
        return new RpcException(-32603, "Internal error", data);
    }

    public static RpcException schemaError(String data) {
        // a kind of internal error -- in the 'implementation-defined server errors' range
        return new RpcException(-32001, "Schema validation error", data);
    }
}
