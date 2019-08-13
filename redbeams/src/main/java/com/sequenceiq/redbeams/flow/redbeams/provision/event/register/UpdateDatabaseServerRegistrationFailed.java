package com.sequenceiq.redbeams.flow.redbeams.provision.event.register;

import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsFailureEvent;

/**
 * The event that occurs when database server registration has failed.
 */
public class UpdateDatabaseServerRegistrationFailed extends RedbeamsFailureEvent {

    public UpdateDatabaseServerRegistrationFailed(Long resourceId, Exception exception) {
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
