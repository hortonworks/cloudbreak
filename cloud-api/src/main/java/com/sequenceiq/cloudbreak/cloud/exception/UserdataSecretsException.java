package com.sequenceiq.cloudbreak.cloud.exception;

public class UserdataSecretsException extends RuntimeException {

    public UserdataSecretsException(String message) {
        super(message);
    }

    public UserdataSecretsException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserdataSecretsException(Throwable cause) {
        super(cause);
    }
}
