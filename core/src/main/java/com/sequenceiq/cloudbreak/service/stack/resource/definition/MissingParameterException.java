package com.sequenceiq.cloudbreak.service.stack.resource.definition;

public class MissingParameterException extends RuntimeException {

    public MissingParameterException(String message) {
        super(message);
    }
}
