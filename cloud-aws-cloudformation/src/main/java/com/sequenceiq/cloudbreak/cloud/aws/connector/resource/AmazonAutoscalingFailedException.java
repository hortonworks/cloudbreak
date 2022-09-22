package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

public class AmazonAutoscalingFailedException extends Exception {

    public AmazonAutoscalingFailedException(String message) {
        super(message);
    }

    public AmazonAutoscalingFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String toString() {
        String name = getClass().getName();
        String message = getLocalizedMessage();
        return message != null ? message : name;
    }
}
