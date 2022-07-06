package com.sequenceiq.redbeams.flow.redbeams.termination.event.deregister;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsFailureEvent;

/**
 * The event that occurs when database server deregistration has failed.
 */
public class DeregisterDatabaseServerFailed extends RedbeamsFailureEvent {

    @JsonCreator
    public DeregisterDatabaseServerFailed(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("exception") Exception e) {

        super(resourceId, e);
    }

    @Override
    public String toString() {
        return "DeregisterDatabaseServerFailed{"
                + "resourceId=" + getResourceId()
                + ", exception=" + getException()
                + '}';
    }
}
