package com.sequenceiq.cloudbreak.rotation.common;

public class SecretRotationException extends RuntimeException {

    public SecretRotationException(String message) {
        super(message);
    }

    public SecretRotationException(Throwable cause) {
        super(cause);
    }

    public SecretRotationException(String message, Throwable cause) {
        super(message, cause);
    }
}
