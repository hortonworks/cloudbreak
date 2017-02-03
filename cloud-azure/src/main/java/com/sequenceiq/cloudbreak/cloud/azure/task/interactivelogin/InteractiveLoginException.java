package com.sequenceiq.cloudbreak.cloud.azure.task.interactivelogin;

/**
 * Created by perdos on 11/17/16.
 */
public class InteractiveLoginException extends Exception {

    public InteractiveLoginException(String message) {
        super(message);
    }

    public InteractiveLoginException(String message, Throwable cause) {
        super(message, cause);
    }
}
