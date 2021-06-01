package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

public class AmazonAutoscalingFailed extends Exception {

    public AmazonAutoscalingFailed(String message) {
        super(message);
    }

    public AmazonAutoscalingFailed(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String toString() {
        String name = getClass().getName();
        String message = getLocalizedMessage();
        return message != null ? message : name;
    }
}
