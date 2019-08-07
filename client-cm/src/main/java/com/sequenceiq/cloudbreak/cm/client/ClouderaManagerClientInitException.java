package com.sequenceiq.cloudbreak.cm.client;

public class ClouderaManagerClientInitException extends Exception {
    public ClouderaManagerClientInitException() {
    }

    public ClouderaManagerClientInitException(String message) {
        super(message);
    }

    public ClouderaManagerClientInitException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClouderaManagerClientInitException(Throwable cause) {
        super(cause);
    }

    public ClouderaManagerClientInitException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
