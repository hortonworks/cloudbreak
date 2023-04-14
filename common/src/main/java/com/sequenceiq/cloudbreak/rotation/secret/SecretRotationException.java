package com.sequenceiq.cloudbreak.rotation.secret;

// TODO WIP
public class SecretRotationException extends RuntimeException {

    private final SecretLocationType failedRotationStep;

    public SecretRotationException(String message, SecretLocationType failedRotationStep) {
        super(message);
        this.failedRotationStep = failedRotationStep;
    }

    public SecretRotationException(String message, Throwable cause, SecretLocationType failedRotationStep) {
        super(message, cause);
        this.failedRotationStep = failedRotationStep;
    }

    public SecretRotationException(Throwable cause, SecretLocationType failedRotationStep) {
        super(cause);
        this.failedRotationStep = failedRotationStep;
    }
}
