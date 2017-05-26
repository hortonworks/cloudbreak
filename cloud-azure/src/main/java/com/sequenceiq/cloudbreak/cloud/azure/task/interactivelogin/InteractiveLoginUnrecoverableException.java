package com.sequenceiq.cloudbreak.cloud.azure.task.interactivelogin;

/**
 * Created by perdos on 11/17/16.
 */
public class InteractiveLoginUnrecoverableException extends Exception {

    public InteractiveLoginUnrecoverableException(String message) {
        super(message);
    }

    public InteractiveLoginUnrecoverableException(String message, Throwable cause) {
        super(message, cause);
    }
}
