package com.sequenceiq.cloudbreak.streaming.model;

public class StreamProcessingException extends Exception {

    public StreamProcessingException(String message) {
        super(message);
    }

    public StreamProcessingException(Throwable cause) {
        super(cause);
    }

    public StreamProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
