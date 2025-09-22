package com.sequenceiq.remoteenvironment;

public class RemoteEnvironmentException extends RuntimeException {
    public RemoteEnvironmentException(String message) {
        super(message);
    }

    public RemoteEnvironmentException(String message, Throwable cause) {
        super(message, cause);
    }
}
