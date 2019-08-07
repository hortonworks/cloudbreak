package com.sequenceiq.cloudbreak.cluster.service;

public class ClusterClientInitException extends Exception {
    public ClusterClientInitException() {
    }

    public ClusterClientInitException(String message) {
        super(message);
    }

    public ClusterClientInitException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClusterClientInitException(Throwable cause) {
        super(cause);
    }

    public ClusterClientInitException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
