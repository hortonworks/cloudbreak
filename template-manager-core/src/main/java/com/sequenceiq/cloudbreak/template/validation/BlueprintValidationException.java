package com.sequenceiq.cloudbreak.template.validation;

public class BlueprintValidationException extends RuntimeException {

    public BlueprintValidationException(String message) {
        super(message);
    }

    public BlueprintValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
