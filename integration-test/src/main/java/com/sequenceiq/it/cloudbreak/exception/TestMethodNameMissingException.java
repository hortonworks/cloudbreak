package com.sequenceiq.it.cloudbreak.exception;

public class TestMethodNameMissingException extends RuntimeException {

    public TestMethodNameMissingException() {
        super("Test method name was not set on TestContext.");
    }
}
