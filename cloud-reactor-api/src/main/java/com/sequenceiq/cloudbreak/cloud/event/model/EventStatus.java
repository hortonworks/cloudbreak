package com.sequenceiq.cloudbreak.cloud.event.model;

public enum EventStatus {
    OK,
    /**
     * It will be retried in case of exception
     */
    FAILED,
    /**
     * It will not be retried even if exception is occurred
     */
    PERMANENTLY_FAILED
}
