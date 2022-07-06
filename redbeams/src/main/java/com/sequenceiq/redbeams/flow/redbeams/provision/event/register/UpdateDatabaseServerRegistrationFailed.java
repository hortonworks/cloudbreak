package com.sequenceiq.redbeams.flow.redbeams.provision.event.register;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsFailureEvent;

/**
 * The event that occurs when database server registration has failed.
 */
public class UpdateDatabaseServerRegistrationFailed extends RedbeamsFailureEvent {

    @JsonCreator
    public UpdateDatabaseServerRegistrationFailed(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("exception") Exception exception) {

        super(resourceId, exception);
    }

    @Override
    public String toString() {
        return "UpdateDatabaseServerRegistrationFailed{"
                + "resourceId=" + getResourceId()
                + ", exception=" + getException()
                + '}';
    }
}
