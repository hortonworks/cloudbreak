package com.sequenceiq.cloudbreak.service.stack.connector.aws;

public class SnsMessageInvalidException extends RuntimeException {

    public SnsMessageInvalidException(String message) {
        super(message);
    }

    SnsMessageInvalidException(String message, Throwable cause) {
        super(message, cause);
    }

}
