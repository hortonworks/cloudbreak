package com.sequenceiq.cloudbreak.auth.altus.exception;

public class UmsAuthenticationException extends RuntimeException {

    public UmsAuthenticationException(String message) {
        super(message);
    }

    public UmsAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }

}
