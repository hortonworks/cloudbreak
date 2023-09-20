package com.sequenceiq.freeipa.flow.freeipa.user.event;

import java.util.Locale;

import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.common.event.Payload;

public class FreeIpaClientResult<R extends FreeIpaClientRequest<?>> implements Payload {

    private EventStatus status;

    private String statusReason;

    private Exception errorDetails;

    private R request;

    public FreeIpaClientResult(R request) {
        init(EventStatus.OK, null, null, request);
    }

    public FreeIpaClientResult(String statusReason, Exception errorDetails, R request) {
        init(EventStatus.FAILED, statusReason, errorDetails, request);
    }

    protected void init(EventStatus status, String statusReason, Exception errorDetails, R request) {
        this.status = status;
        this.statusReason = statusReason;
        this.errorDetails = errorDetails;
        this.request = request;
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

    public R getRequest() {
        return request;
    }

    @Override
    public String toString() {
        return "FreeIpaResult{"
                + "status=" + status
                + ", statusReason='" + statusReason + '\''
                + ", errorDetails=" + errorDetails
                + ", request=" + request
                + '}';
    }

    @Override
    public Long getResourceId() {
        return request.getResourceId();
    }
}
