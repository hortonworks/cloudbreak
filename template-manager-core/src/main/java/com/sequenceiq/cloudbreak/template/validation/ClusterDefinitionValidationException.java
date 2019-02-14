package com.sequenceiq.cloudbreak.template.validation;

public class ClusterDefinitionValidationException extends RuntimeException {

    public ClusterDefinitionValidationException(String message) {
        super(message);
    }

    public ClusterDefinitionValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
