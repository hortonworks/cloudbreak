package com.sequenceiq.it.cloudbreak.exception;

public class TestFailException extends RuntimeException {

    public TestFailException(String message) {
        super(message);
    }

    public TestFailException(String message, Throwable cause) {
        super(message, cause);
    }

}