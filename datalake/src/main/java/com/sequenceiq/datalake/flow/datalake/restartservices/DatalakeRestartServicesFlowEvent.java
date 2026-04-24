package com.sequenceiq.datalake.flow.datalake.restartservices;

import com.sequenceiq.datalake.flow.datalake.restartservices.event.DatalakeRestartServicesFailedEvent;
import com.sequenceiq.datalake.flow.datalake.restartservices.event.DatalakeRestartServicesStartEvent;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum DatalakeRestartServicesFlowEvent implements FlowEvent {
    DATALAKE_RESTART_SERVICES_START_EVENT(DatalakeRestartServicesStartEvent.class),
    DATALAKE_RESTART_SERVICES_IN_PROGRESS_EVENT,
    DATALAKE_RESTART_SERVICES_FINISHED_EVENT,
    DATALAKE_RESTART_SERVICES_FINALIZED_EVENT,
    DATALAKE_RESTART_SERVICES_FAILED_EVENT(DatalakeRestartServicesFailedEvent.class),
    DATALAKE_RESTART_SERVICES_FAILED_HANDLED_EVENT;

    private final String event;

    DatalakeRestartServicesFlowEvent(Class<?> clazz) {
        event = EventSelectorUtil.selector(clazz);
    }

    DatalakeRestartServicesFlowEvent() {
        event = name();
    }

    @Override
    public String event() {
        return event;
    }
}
