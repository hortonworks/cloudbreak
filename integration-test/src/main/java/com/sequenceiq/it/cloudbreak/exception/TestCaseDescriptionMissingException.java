package com.sequenceiq.it.cloudbreak.exception;

public class TestCaseDescriptionMissingException extends RuntimeException {

    public TestCaseDescriptionMissingException() {
        super("Every testcase needs a testcase description. Please be kind and add one.");
    }

}