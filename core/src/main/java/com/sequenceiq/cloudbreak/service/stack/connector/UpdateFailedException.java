package com.sequenceiq.cloudbreak.service.stack.connector;

public class UpdateFailedException extends Exception {
    public UpdateFailedException(String message) {
        super(message);
    }

    public UpdateFailedException(Throwable cause) {
        super(cause);
    }
}
