package com.sequenceiq.datalake.flow.stop;

import com.sequenceiq.datalake.flow.stop.event.RdsStopSuccessEvent;
import com.sequenceiq.datalake.flow.stop.event.SdxStopFailedEvent;
import com.sequenceiq.datalake.flow.stop.event.SdxStopSuccessEvent;
import com.sequenceiq.flow.core.FlowEvent;

public enum SdxStopEvent implements FlowEvent {

    SDX_STOP_EVENT(),
    SDX_STOP_IN_PROGRESS_EVENT(),
    SDX_STOP_ALL_DATAHUB_EVENT(),
    SDX_STOP_RDS_EVENT(),
    SDX_STOP_RDS_FINISHED_EVENT(RdsStopSuccessEvent.class),
    SDX_STOP_SUCCESS_EVENT(SdxStopSuccessEvent.class),
    SDX_STOP_FAILED_EVENT(SdxStopFailedEvent.class),
    SDX_STOP_FAILED_HANDLED_EVENT(),
    SDX_STOP_FINALIZED_EVENT();

    private final String event;

    SdxStopEvent() {
        this.event = name();
    }

    SdxStopEvent(Class eventClass) {
        this.event = eventClass.getSimpleName();
    }

    @Override
    public String event() {
        return event;
    }

}
