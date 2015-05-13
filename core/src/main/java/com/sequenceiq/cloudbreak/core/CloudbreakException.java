package com.sequenceiq.cloudbreak.core;

public class CloudbreakException extends Exception {

    public CloudbreakException(String message) {
        super(message);
    }

    public CloudbreakException(String message, Throwable cause) {
        super(message, cause);
    }

    public CloudbreakException(Throwable cause) {
        super(cause);
    }
}
