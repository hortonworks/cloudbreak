package com.sequenceiq.cloudbreak.blueprint;


public class BlueprintProcessingException extends RuntimeException {

    public BlueprintProcessingException(String message) {
        super(message);
    }

    public BlueprintProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public BlueprintProcessingException(Throwable cause) {
        super(cause);
    }
}
