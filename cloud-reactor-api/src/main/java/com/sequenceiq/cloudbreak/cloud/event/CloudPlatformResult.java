package com.sequenceiq.cloudbreak.cloud.event;

import java.util.Locale;

import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;

public class CloudPlatformResult implements Selectable {

    private final Long resourceId;

    private EventStatus status;

    private String statusReason;

    private Exception errorDetails;

    public CloudPlatformResult(Long resourceId) {
        this.resourceId = resourceId;
        init(EventStatus.OK, null, null);
    }

    public CloudPlatformResult(String statusReason, Exception errorDetails, Long resourceId) {
        this(EventStatus.FAILED, statusReason, errorDetails, resourceId);
    }

    public CloudPlatformResult(EventStatus status, String statusReason, Exception errorDetails, Long resourceId) {
        this.resourceId = resourceId;
        init(status, statusReason, errorDetails);
    }

    protected void init(EventStatus status, String statusReason, Exception errorDetails) {
        this.status = status;
        this.statusReason = statusReason;
        this.errorDetails = errorDetails;
    }

    public static String selector(Class<?> clazz) {
        return clazz.getSimpleName().toUpperCase(Locale.ROOT);
    }

    public static String failureSelector(Class<?> clazz) {
        return clazz.getSimpleName().toUpperCase(Locale.ROOT) + "_ERROR";
    }

    public String selector() {
        return status == EventStatus.OK ? selector(getClass()) : failureSelector(getClass());
    }

    public EventStatus getStatus() {
        return status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public Exception getErrorDetails() {
        return errorDetails;
    }

    @Override
    public Exception getException() {
        return getErrorDetails();
    }

    @Override
    public String toString() {
        return "CloudPlatformResult{"
                + "status=" + status
                + ", statusReason='" + statusReason + '\''
                + ", errorDetails=" + errorDetails
                + ", resourceId=" + resourceId
                + '}';
    }

    @Override
    public Long getResourceId() {
        return resourceId;
    }
}