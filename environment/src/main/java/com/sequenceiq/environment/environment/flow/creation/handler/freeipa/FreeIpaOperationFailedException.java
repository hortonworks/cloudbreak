package com.sequenceiq.environment.environment.flow.creation.handler.freeipa;

public class FreeIpaOperationFailedException extends RuntimeException {

    public FreeIpaOperationFailedException(String message) {
        super(message);
    }

    public FreeIpaOperationFailedException(String message, Throwable cause) {
        super(message, cause);
    }

}
