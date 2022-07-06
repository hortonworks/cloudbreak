package com.sequenceiq.redbeams.flow.redbeams.stop.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsFailureEvent;

/**
 * The event that occurs when database server stop has failed.
 */
public class StopDatabaseServerFailed extends RedbeamsFailureEvent {

    @JsonCreator
    public StopDatabaseServerFailed(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("exception") Exception e) {

        super(resourceId, e);
    }

    @Override
    public String toString() {
        return "StopDatabaseServerFailed{"
                + "resourceId=" + getResourceId()
                + ", exception=" + getException()
                + '}';
    }
}
