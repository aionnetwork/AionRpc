package org.aion.api.codegen;

import java.util.List;

public class JavaMethodCall {
    private List<String> inputTypes;
    private String outputType;
    private String methodName;

    public List<String> getInputTypes() {
        return inputTypes;
    }

    public String getOutputType() {
        return outputType;
    }

    public String getMethodName() {
        return methodName;
    }

    public JavaMethodCall(List<String> inputTypes, String outputType, String methodName) {
        this.inputTypes = inputTypes;
        this.outputType = outputType;
        this.methodName = methodName;
    }
}
