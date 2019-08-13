package com.sequenceiq.redbeams.flow.redbeams.provision;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.allocate.AllocateDatabaseServerFailed;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.allocate.AllocateDatabaseServerSuccess;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.register.UpdateDatabaseServerRegistrationFailed;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.register.UpdateDatabaseServerRegistrationSuccess;

public enum RedbeamsProvisionEvent implements FlowEvent {
    REDBEAMS_PROVISION_EVENT("REDBEAMS_PROVISION_EVENT"),
    ALLOCATE_DATABASE_SERVER_FINISHED_EVENT(EventSelectorUtil.selector(AllocateDatabaseServerSuccess.class)),
    ALLOCATE_DATABASE_SERVER_FAILED_EVENT(EventSelectorUtil.selector(AllocateDatabaseServerFailed.class)),
    UPDATE_DATABASE_SERVER_REGISTRATION_FINISHED_EVENT(EventSelectorUtil.selector(UpdateDatabaseServerRegistrationSuccess.class)),
    UPDATE_DATABASE_SERVER_REGISTRATION_FAILED_EVENT(EventSelectorUtil.selector(UpdateDatabaseServerRegistrationFailed.class)),
    REDBEAMS_PROVISION_FAILED_EVENT("REDBEAMS_PROVISION_FAILED_EVENT"),
    REDBEAMS_PROVISION_FINISHED_EVENT("REDBEAMS_PROVISION_FINISHED_EVENT"),
    REDBEAMS_PROVISION_FAILURE_HANDLED_EVENT("REDBEAMS_PROVISION_FAILURE_HANDLED_EVENT");

    private final String event;

    RedbeamsProvisionEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
