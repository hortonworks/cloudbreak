package com.sequenceiq.redbeams.flow.redbeams.provision.event.register;

import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsFailureEvent;

/**
 * The event that occurs when database server registration has failed.
 */
public class RegisterDatabaseServerFailed extends RedbeamsFailureEvent {

    public RegisterDatabaseServerFailed(Long resourceId, Exception exception) {
        super(resourceId, exception);
    }

    @Override
    public String toString() {
        return "RegisterDatabaseServerFailed{"
                + "resourceId=" + getResourceId()
                + ", exception=" + getException()
                + '}';
    }
}
