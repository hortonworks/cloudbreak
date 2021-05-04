package com.sequenceiq.cloudbreak.cloud.azure.rest;

public class AzureRestResponseException extends RuntimeException {

    public AzureRestResponseException(String message) {
        super(message);
    }

    public AzureRestResponseException(String message, Throwable t) {
        super(message, t);
    }
}
