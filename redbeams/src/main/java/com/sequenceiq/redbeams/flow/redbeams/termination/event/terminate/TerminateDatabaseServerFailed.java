package com.sequenceiq.redbeams.flow.redbeams.termination.event.terminate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsFailureEvent;

/**
 * The event that occurs when a database server termination has failed.
 */
public class TerminateDatabaseServerFailed extends RedbeamsFailureEvent {

    @JsonCreator
    public TerminateDatabaseServerFailed(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("forced") boolean force) {

        super(resourceId, exception, force);
    }

    @Override
    public String toString() {
        return "TerminateDatabaseServerFailed{"
                + "resourceId=" + getResourceId()
                + ", exception=" + getException()
                + '}';
    }
}
