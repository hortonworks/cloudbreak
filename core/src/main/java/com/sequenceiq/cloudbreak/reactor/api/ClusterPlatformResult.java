package com.sequenceiq.cloudbreak.reactor.api;

import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;

public abstract class ClusterPlatformResult<R extends ClusterPlatformRequest> implements Payload, Selectable {

    private EventStatus status;
    private String statusReason;
    private Exception errorDetails;
    private R request;

    public ClusterPlatformResult(R request) {
        init(EventStatus.OK, null, null, request);
    }

    public ClusterPlatformResult(String statusReason, Exception errorDetails, R request) {
        init(EventStatus.FAILED, statusReason, errorDetails, request);
    }

    protected void init(EventStatus status, String statusReason, Exception errorDetails, R request) {
        this.status = status;
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

    @Override
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
    public Long getStackId() {
        return request.getStackId();
    }
}
