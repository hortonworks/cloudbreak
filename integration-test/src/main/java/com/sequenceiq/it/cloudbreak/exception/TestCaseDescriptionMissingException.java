package com.sequenceiq.it.cloudbreak.exception;

public class TestCaseDescriptionMissingException extends RuntimeException {

    public TestCaseDescriptionMissingException(String message) {
        super(message + "| Missing testcase description. Please be kind and add one.");
    }

}