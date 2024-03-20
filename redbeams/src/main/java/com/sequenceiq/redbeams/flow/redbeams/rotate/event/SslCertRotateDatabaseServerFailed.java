package com.sequenceiq.redbeams.flow.redbeams.rotate.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsFailureEvent;

/**
 * The event that occurs when database server cert rotated has failed.
 */
public class SslCertRotateDatabaseServerFailed extends RedbeamsFailureEvent {

    @JsonCreator
    public SslCertRotateDatabaseServerFailed(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("exception") Exception e) {

        super(resourceId, e);
    }

    @Override
    public String toString() {
        return "SslCertRotateDatabaseServerFailed{"
                + "resourceId=" + getResourceId()
                + ", exception=" + getException()
                + '}';
    }
}
