package com.sequenceiq.cloudbreak.rotation.secret;

// TODO WIP
public class SecretRotationException extends RuntimeException {

    private final SecretRotationStep failedRotationStep;

    public SecretRotationException(String message, SecretRotationStep failedRotationStep) {
        super(message);
        this.failedRotationStep = failedRotationStep;
    }

    public SecretRotationException(String message, Throwable cause, SecretRotationStep failedRotationStep) {
        super(message, cause);
        this.failedRotationStep = failedRotationStep;
    }

    public SecretRotationException(Throwable cause, SecretRotationStep failedRotationStep) {
        super(cause);
        this.failedRotationStep = failedRotationStep;
    }
}
