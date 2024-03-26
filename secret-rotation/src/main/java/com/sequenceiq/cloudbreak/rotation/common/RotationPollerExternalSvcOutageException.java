package com.sequenceiq.cloudbreak.rotation.common;

public class RotationPollerExternalSvcOutageException extends RuntimeException {

    public RotationPollerExternalSvcOutageException(String message) {
        super(message);
    }

    public RotationPollerExternalSvcOutageException(Throwable cause) {
        super(cause);
    }

    public RotationPollerExternalSvcOutageException(String message, Throwable cause) {
        super(message, cause);
    }
}
