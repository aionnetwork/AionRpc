package org.aion.api.schema;

public class ErrorUsage {
    private final RpcError error;
    private final String reason;

    public ErrorUsage(RpcError errorName, String reason) {
        this.error = errorName;
        this.reason = reason;
    }

    public RpcError getError() {
        return error;
    }

    public String getReason() {
        return reason;
    }
}
