package org.aion.api.schema;

public class ErrorUsage {
    private final String errorName;
    private final String reason;

    public ErrorUsage(String errorName, String reason) {
        this.errorName = errorName;
        this.reason = reason;
    }

    public String getErrorName() {
        return errorName;
    }

    public String getReason() {
        return reason;
    }
}
