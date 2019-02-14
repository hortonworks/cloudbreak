package com.sequenceiq.cloudbreak.template;

public class ClusterDefinitionProcessingException extends RuntimeException {
    public ClusterDefinitionProcessingException(String message) {
        super(message);
    }

    public ClusterDefinitionProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
