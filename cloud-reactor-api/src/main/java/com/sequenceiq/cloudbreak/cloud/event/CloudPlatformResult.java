package com.sequenceiq.cloudbreak.cloud.event;

import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;

public class CloudPlatformResult<R extends CloudPlatformRequest> {

    private EventStatus status;
    private String statusReason;
    private Exception errorDetails;
    private R request;

    public CloudPlatformResult(R request) {
        this.status = EventStatus.OK;
        this.request = request;
    }

    public CloudPlatformResult(String statusReason, Exception errorDetails, R request) {
        this.status = EventStatus.FAILED;
        this.statusReason = statusReason;
        this.errorDetails = errorDetails;
        this.request = request;
    }

    public static String selector(Class clazz) {
        return clazz.getSimpleName().toUpperCase();
    }

    public static String failureSelector(Class clazz) {
        return clazz.getSimpleName().toUpperCase() + "_ERROR";
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

    public R getRequest() {
        return request;
    }

    @Override
    public String toString() {
        return "CloudPlatformResult{"
                + "status=" + status
                + ", statusReason='" + statusReason + '\''
                + ", errorDetails=" + errorDetails
                + ", request=" + request
                + '}';
    }
}
