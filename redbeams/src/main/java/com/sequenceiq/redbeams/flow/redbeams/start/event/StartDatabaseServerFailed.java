package com.sequenceiq.redbeams.flow.redbeams.start.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsFailureEvent;

/**
 * The event that occurs when database server start has failed.
 */
public class StartDatabaseServerFailed extends RedbeamsFailureEvent {

    @JsonCreator
    public StartDatabaseServerFailed(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("exception") Exception e) {

        super(resourceId, e);
    }

    @Override
    public String toString() {
        return "StartDatabaseServerFailed{"
                + "resourceId=" + getResourceId()
                + ", exception=" + getException()
                + '}';
    }
}
