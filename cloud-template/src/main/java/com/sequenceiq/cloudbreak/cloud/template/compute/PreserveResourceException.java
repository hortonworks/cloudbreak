package com.sequenceiq.cloudbreak.cloud.template.compute;

public class PreserveResourceException extends Exception {

    public PreserveResourceException(String message) {
        super(message);
    }

    public PreserveResourceException(String message, Throwable cause) {
        super(message, cause);
    }

}
