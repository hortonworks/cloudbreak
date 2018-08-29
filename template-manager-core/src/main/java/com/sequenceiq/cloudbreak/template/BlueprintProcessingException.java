package com.sequenceiq.cloudbreak.template;

public class BlueprintProcessingException extends RuntimeException {
    public BlueprintProcessingException(String message) {
        super(message);
    }

    public BlueprintProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
