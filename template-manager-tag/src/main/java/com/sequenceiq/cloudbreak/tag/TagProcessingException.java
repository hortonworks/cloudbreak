package com.sequenceiq.cloudbreak.tag;

public class TagProcessingException extends RuntimeException {
    public TagProcessingException(String message) {
        super(message);
    }

    public TagProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
