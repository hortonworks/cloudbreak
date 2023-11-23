package com.sequenceiq.cloudbreak.auth.altus.exception;

public class UmsErrorException extends RuntimeException {

    public UmsErrorException(String message) {
        super(message);
    }

    public UmsErrorException(String message, Throwable cause) {
        super(message, cause);
    }
}
