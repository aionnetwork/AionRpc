package org.aion.api.codegen;

import java.util.List;

public class JavaInterfaceMethodDeclaration {
    private final String methodName;
    private final String returnType;
    private final List<String> args;
    private final List<String> exceptions;

    public JavaInterfaceMethodDeclaration(String methodName,
                                          String returnType,
                                          List<String> args,
                                          List<String> exceptions) {
        this.methodName = methodName;
        this.returnType = returnType;
        this.args = args;
        this.exceptions = exceptions;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getReturnType() {
        return returnType;
    }

    public List<String> getArgs() {
        return args;
    }

    public List<String> getExceptions() {
        return exceptions;
    }
}
