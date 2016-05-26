package com.sequenceiq.cloudbreak.service.cluster.flow.blueprint;


public class BlueprintProcessingException extends RuntimeException {

    public BlueprintProcessingException(String message) {
        super(message);
    }

    public BlueprintProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
