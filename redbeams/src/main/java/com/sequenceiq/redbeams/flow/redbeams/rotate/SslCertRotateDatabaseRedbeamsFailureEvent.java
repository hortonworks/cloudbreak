package com.sequenceiq.redbeams.flow.redbeams.rotate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsFailureEvent;

public class SslCertRotateDatabaseRedbeamsFailureEvent extends RedbeamsFailureEvent {

    private final boolean onlyCertificateUpdate;

    public SslCertRotateDatabaseRedbeamsFailureEvent(Long resourceId, Exception exception, boolean onlyCertificateUpdate) {
        super(resourceId, exception);
        this.onlyCertificateUpdate = onlyCertificateUpdate;
    }

    @JsonCreator
    public SslCertRotateDatabaseRedbeamsFailureEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("forced") boolean forced,
            @JsonProperty("onlyCertificateUpdate") boolean onlyCertificateUpdate) {
        super(selector, resourceId, exception, forced);
        this.onlyCertificateUpdate = onlyCertificateUpdate;
    }

    public boolean isOnlyCertificateUpdate() {
        return onlyCertificateUpdate;
    }

    @Override
    public String toString() {
        return "SslCertRotateDatabaseRedbeamsFailureEvent{" +
                "onlyCertificateUpdate=" + onlyCertificateUpdate +
                "} " + super.toString();
    }
}
