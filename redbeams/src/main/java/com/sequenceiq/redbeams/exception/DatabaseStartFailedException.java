package com.sequenceiq.redbeams.exception;

public class DatabaseStartFailedException extends RuntimeException {

    public DatabaseStartFailedException(String message) {
        super(message);
    }

    public DatabaseStartFailedException(String message, Throwable cause) {
        super(message, cause);
    }

}
