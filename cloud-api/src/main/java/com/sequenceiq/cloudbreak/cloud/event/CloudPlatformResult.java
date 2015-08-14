package com.sequenceiq.cloudbreak.cloud.event;

import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;

public class CloudPlatformResult {

    private EventStatus status;
    private String statusReason;
    private Exception errorDetails;
    private CloudPlatformRequest<?> request;

    public CloudPlatformResult(CloudPlatformRequest<?> request) {
        this.status = EventStatus.OK;
        this.request = request;
    }

    public CloudPlatformResult(String statusReason, Exception errorDetails, CloudPlatformRequest<?> request) {
        this.status = EventStatus.FAILED;
        this.statusReason = statusReason;
        this.errorDetails = errorDetails;
        this.request = request;
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

    public CloudPlatformRequest<?> getRequest() {
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
