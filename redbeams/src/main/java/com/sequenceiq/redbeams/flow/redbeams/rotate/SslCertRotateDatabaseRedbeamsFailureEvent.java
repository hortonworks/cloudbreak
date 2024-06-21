package com.sequenceiq.redbeams.flow.redbeams.rotate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

public class SslCertRotateDatabaseRedbeamsFailureEvent extends RedbeamsEvent {

    private final Exception exception;

    private final boolean onlyCertificateUpdate;

    public SslCertRotateDatabaseRedbeamsFailureEvent(Long resourceId, Exception exception, boolean onlyCertificateUpdate) {
        super(resourceId);
        this.exception = exception;
        this.onlyCertificateUpdate = onlyCertificateUpdate;
    }

    public SslCertRotateDatabaseRedbeamsFailureEvent(Long resourceId, Exception exception, boolean force, boolean onlyCertificateUpdate) {
        super(resourceId, force);
        this.exception = exception;
        this.onlyCertificateUpdate = onlyCertificateUpdate;
    }

    @JsonCreator
    public SslCertRotateDatabaseRedbeamsFailureEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("forced") boolean forced,
            @JsonProperty("onlyCertificateUpdate") boolean onlyCertificateUpdate) {
        super(selector, resourceId, forced);
        this.exception = exception;
        this.onlyCertificateUpdate = onlyCertificateUpdate;
    }

    public Exception getException() {
        return exception;
    }

    public boolean isOnlyCertificateUpdate() {
        return onlyCertificateUpdate;
    }

    @Override
    public String toString() {
        return "RedbeamsFailureEvent{" +
                "exception=" + exception +
                "onlyCertificateUpdate=" + onlyCertificateUpdate +
                "} " + super.toString();
    }
}
