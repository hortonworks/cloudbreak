package com.sequenceiq.it.cloudbreak.exception;

public class StoreFileException extends RuntimeException {

    public StoreFileException(String message) {
        super(message);
    }

    public StoreFileException(String message, Throwable cause) {
        super(message, cause);
    }

    public StoreFileException(Throwable cause) {
        super(cause);
    }
}
