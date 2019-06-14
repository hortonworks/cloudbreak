package com.sequenceiq.redbeams.flow.redbeams.provision.event.register;

import com.sequenceiq.redbeams.flow.redbeams.provision.RedbeamsEvent;

/**
 * A request for registering a database server after allocation.
 */
public class RegisterDatabaseServerRequest extends RedbeamsEvent {
    public RegisterDatabaseServerRequest(Long resourceId) {
        super(resourceId);
    }
}
