package com.sequenceiq.cloudbreak.auth;

public class SSLConnectionException extends RuntimeException {

    public SSLConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public SSLConnectionException(String message) {
        super(message);
    }
}


