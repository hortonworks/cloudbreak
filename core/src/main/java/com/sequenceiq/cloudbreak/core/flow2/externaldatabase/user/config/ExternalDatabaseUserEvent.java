package com.sequenceiq.cloudbreak.core.flow2.externaldatabase.user.config;

import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseSelectableEvent;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.user.ExternalDatabaseUserFlowStartEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase.UserOperationExternalDatabaseFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase.UserOperationExternalDatabaseResult;
import com.sequenceiq.flow.core.FlowEvent;

public enum ExternalDatabaseUserEvent implements FlowEvent {

    START_EXTERNAL_DATABASE_USER_OPERATION_EVENT(ExternalDatabaseSelectableEvent.selector(ExternalDatabaseUserFlowStartEvent.class)),
    EXTERNAL_DATABASE_USER_OPERATION_SUCCESS_EVENT(ExternalDatabaseSelectableEvent.selector(UserOperationExternalDatabaseResult.class)),
    EXTERNAL_DATABASE_USER_OPERATION_FAILED_EVENT(ExternalDatabaseSelectableEvent.selector(UserOperationExternalDatabaseFailed.class)),
    EXTERNAL_DATABASE_USER_OPERATION_FINISHED_EVENT,
    EXTERNAL_DATABASE_USER_OPERATION_FAILURE_HANDLED_EVENT;

    private final String event;

    ExternalDatabaseUserEvent() {
        this.event = name();
    }

    ExternalDatabaseUserEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
