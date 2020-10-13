package com.sequenceiq.cloudbreak.cm.client.tracing;

public class TestStackFrame implements StackWalker.StackFrame {

    private final String className;

    private final String methodName;

    public TestStackFrame(String className, String methodName) {
        this.className = className;
        this.methodName = methodName;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public String getMethodName() {
        return methodName;
    }

    @Override
    public Class<?> getDeclaringClass() {
        return null;
    }

    @Override
    public int getByteCodeIndex() {
        return 0;
    }

    @Override
    public String getFileName() {
        return null;
    }

    @Override
    public int getLineNumber() {
        return 0;
    }

    @Override
    public boolean isNativeMethod() {
        return false;
    }

    @Override
    public StackTraceElement toStackTraceElement() {
        return null;
    }
}
