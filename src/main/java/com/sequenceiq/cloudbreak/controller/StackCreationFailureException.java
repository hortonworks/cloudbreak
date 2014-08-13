package com.sequenceiq.cloudbreak.controller;

public class StackCreationFailureException extends RuntimeException {

    public StackCreationFailureException(Exception ex) {
        super(ex);
    }

    public StackCreationFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}
