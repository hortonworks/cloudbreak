package com.sequenceiq.redbeams.flow.redbeams.provision.event.register;

import com.sequenceiq.redbeams.flow.redbeams.provision.RedbeamsEvent;

/**
 * This event occurs when a database has been successfully registered.
 */
public class RegisterDatabaseServerSuccess extends RedbeamsEvent {
    public RegisterDatabaseServerSuccess(Long resourceId) {
        super(resourceId);
    }
}
