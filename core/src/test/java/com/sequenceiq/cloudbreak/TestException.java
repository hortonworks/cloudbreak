package com.sequenceiq.cloudbreak;

public class TestException extends RuntimeException {
    public TestException(Throwable cause) {
        super(cause);
    }

    public TestException(String message) {
        super(message);
    }

    public TestException(String message, Throwable cause) {
        super(message, cause);
    }
}
