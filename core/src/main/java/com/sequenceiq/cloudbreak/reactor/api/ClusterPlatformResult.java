package com.sequenceiq.cloudbreak.reactor.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.event.EventSelectorUtil;

public abstract class ClusterPlatformResult<R extends ClusterPlatformRequest> implements Selectable {

    private EventStatus status;

    private String statusReason;

    private Exception errorDetails;

    private R request;

    protected ClusterPlatformResult(R request) {
        init(EventStatus.OK, null, null, request);
    }

    protected ClusterPlatformResult(String statusReason, Exception errorDetails, R request) {
        init(EventStatus.FAILED, statusReason, errorDetails, request);
    }

    protected ClusterPlatformResult(EventStatus status, String statusReason, Exception errorDetails, R request) {
        init(status, statusReason, errorDetails, request);
    }

    protected void init(EventStatus status, String statusReason, Exception errorDetails, R request) {
        this.status = status;
        this.statusReason = statusReason;
        this.errorDetails = errorDetails;
        this.request = request;
    }

    @Override
    public String selector() {
        return status == EventStatus.OK ? EventSelectorUtil.selector(getClass()) : EventSelectorUtil.failureSelector(getClass());
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

    /**
     * Exposes {@link #errorDetails} through {@link com.sequenceiq.cloudbreak.common.event.Payload#getException()} so the flow engine records
     * it as the flow log reason. Ignored for JSON, because {@code errorDetails} already carries the same exception in the payload.
     */
    @JsonIgnore
    @Override
    public Exception getException() {
        return errorDetails;
    }

    public R getRequest() {
        return request;
    }

    @Override
    public Long getResourceId() {
        return request.getResourceId();
    }

    @Override
    public String toString() {
        return "ClusterPlatformResult{"
                + "status=" + status
                + ", statusReason='" + statusReason + '\''
                + ", errorDetails=" + errorDetails
                + '}';
    }
}
