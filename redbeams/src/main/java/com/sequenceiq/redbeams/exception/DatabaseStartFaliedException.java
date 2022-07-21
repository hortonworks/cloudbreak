package com.sequenceiq.redbeams.exception;

public class DatabaseStartFaliedException extends RuntimeException {

    public DatabaseStartFaliedException(String message) {
        super(message);
    }

    public DatabaseStartFaliedException(String message, Throwable cause) {
        super(message, cause);
    }

}
