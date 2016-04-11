package com.sequenceiq.cloudbreak.core.flow.handlers;

import com.sequenceiq.cloudbreak.cloud.event.ClusterPayload;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;

public class AmbariClusterResult<R extends AmbariClusterRequest> implements ClusterPayload {

    private EventStatus status;
    private String statusReason;
    private Exception errorDetails;
    private R request;

    public AmbariClusterResult(R request) {
        this.status = EventStatus.OK;
        this.request = request;
    }

    public AmbariClusterResult(String statusReason, Exception errorDetails, R request) {
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
                + "request=" + request
                + '}';
    }

    @Override
    public Long getStackId() {
        return request.getClusterContext().getStackId();
    }

    @Override
    public Long getClusterId() {
        return request.getClusterContext().getClusterId();
    }
}
