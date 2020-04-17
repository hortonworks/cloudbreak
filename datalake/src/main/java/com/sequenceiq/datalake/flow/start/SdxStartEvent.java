package com.sequenceiq.datalake.flow.start;

import com.sequenceiq.datalake.flow.start.event.RdsStartSuccessEvent;
import com.sequenceiq.datalake.flow.start.event.SdxStartFailedEvent;
import com.sequenceiq.datalake.flow.start.event.SdxStartSuccessEvent;
import com.sequenceiq.datalake.flow.start.event.SdxSyncSuccessEvent;
import com.sequenceiq.flow.core.FlowEvent;

public enum SdxStartEvent implements FlowEvent {

    SDX_START_EVENT(),
    SDX_START_RDS_EVENT(),
    SDX_START_RDS_FINISHED_EVENT(RdsStartSuccessEvent.class),
    SDX_SYNC_EVENT(),
    SDX_SYNC_FINISHED_EVENT(SdxSyncSuccessEvent.class),
    SDX_START_IN_PROGRESS_EVENT(),
    SDX_START_SUCCESS_EVENT(SdxStartSuccessEvent.class),
    SDX_START_FAILED_EVENT(SdxStartFailedEvent.class),
    SDX_START_FAILED_HANDLED_EVENT(),
    SDX_START_FINALIZED_EVENT();

    private final String event;

    SdxStartEvent() {
        event = name();
    }

    SdxStartEvent(Class eventClass) {
        this.event = eventClass.getSimpleName();
    }

    @Override
    public String event() {
        return event;
    }

}
