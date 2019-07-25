package org.aion.api.codegen;

import java.util.List;

public class JavaInterfaceMethodDeclaration {
    private final String methodName;
    private final String returnType;
    private final List<String> args;

    public JavaInterfaceMethodDeclaration(String methodName, String returnType, List<String> args) {
        this.methodName = methodName;
        this.returnType = returnType;
        this.args = args;
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
}
