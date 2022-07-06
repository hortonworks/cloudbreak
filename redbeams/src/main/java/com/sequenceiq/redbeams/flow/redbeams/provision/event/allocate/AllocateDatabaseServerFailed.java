package com.sequenceiq.redbeams.flow.redbeams.provision.event.allocate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsFailureEvent;

/**
 * The event that occurs when a database server allocation has failed.
 */
public class AllocateDatabaseServerFailed extends RedbeamsFailureEvent {

    @JsonCreator
    public AllocateDatabaseServerFailed(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("exception") Exception exception) {

        super(resourceId, exception);
    }

    @Override
    public String toString() {
        return "AllocateDatabaseServerFailed{"
                + "resourceId=" + getResourceId()
                + ", exception=" + getException()
                + '}';
    }
}
