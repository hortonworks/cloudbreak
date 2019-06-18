package com.sequenceiq.redbeams.flow.redbeams.provision.event.register;

import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

/**
 * The event that occurs when database server registration has failed.
 */
public class RegisterDatabaseServerFailed extends RedbeamsEvent {
    public RegisterDatabaseServerFailed(Long resourceId) {
        super(resourceId);
    }
}
