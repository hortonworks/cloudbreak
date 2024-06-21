package com.sequenceiq.redbeams.flow.redbeams.rotate.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.redbeams.flow.redbeams.rotate.SslCertRotateDatabaseRedbeamsFailureEvent;

/**
 * The event that occurs when database server cert rotated has failed.
 */
public class SslCertRotateDatabaseServerFailed extends SslCertRotateDatabaseRedbeamsFailureEvent {

    @JsonCreator
    public SslCertRotateDatabaseServerFailed(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("exception") Exception e,
            @JsonProperty("onlyCertificateUpdate") boolean onlyCertificateUpdate) {
        super(resourceId, e, onlyCertificateUpdate);
    }

    @Override
    public String toString() {
        return "SslCertRotateDatabaseServerFailed{"
                + "resourceId=" + getResourceId()
                + ", exception=" + getException()
                + ", onlyCertificateUpdate=" + isOnlyCertificateUpdate()
                + '}';
    }
}
