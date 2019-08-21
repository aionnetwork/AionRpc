package org.aion.api.prototype;

public class UnauthorizedException extends org.aion.api.RpcException {

    public UnauthorizedException(String data) {
        super(10001, "Unauthorized", data);
    }
}
