package org.aion.api.server.rpc2.autogen.errors;

public class ${errorName}RpcException extends org.aion.api.RpcException {

    public ${errorName}RpcException(String data) {
        super(${code?c}, "${message}", data);
    }
}
