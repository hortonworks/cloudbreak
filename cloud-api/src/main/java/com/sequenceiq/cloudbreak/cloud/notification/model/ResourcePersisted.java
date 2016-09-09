package com.sequenceiq.cloudbreak.cloud.notification.model;

/**
 * Used for confirming that a resource has been persisted
 */
public class ResourcePersisted {

    private String statusReason;

    private Exception exception;

    public ResourcePersisted() {

    }

    public ResourcePersisted(String statusReason, Exception exception) {
        this.statusReason = statusReason;
        this.exception = exception;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public Exception getException() {
        return exception;
    }
}
