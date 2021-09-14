package com.sequenceiq.datalake.flow.datalake.cmsync;

import com.sequenceiq.datalake.flow.datalake.cmsync.event.SdxCmSyncFailedEvent;
import com.sequenceiq.datalake.flow.datalake.cmsync.event.SdxCmSyncWaitEvent;
import com.sequenceiq.datalake.flow.datalake.cmsync.event.SdxCmSyncStartEvent;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum SdxCmSyncEvent implements FlowEvent {
    SDX_CM_SYNC_START_EVENT(EventSelectorUtil.selector(SdxCmSyncStartEvent.class)),
    SDX_CM_SYNC_IN_PROGRESS_EVENT,
    SDX_CM_SYNC_WAIT_EVENT(EventSelectorUtil.selector(SdxCmSyncWaitEvent.class)),
    SDX_CM_SYNC_FINISHED_EVENT,
    SDX_CM_SYNC_FAILED_EVENT(EventSelectorUtil.selector(SdxCmSyncFailedEvent.class)),
    SDX_CM_SYNC_FAILED_HANDLED_EVENT,
    SDX_CM_SYNC_FINALIZED_EVENT;

    private final String event;

    SdxCmSyncEvent(String event) {
        this.event = event;
    }

    SdxCmSyncEvent() {
        this.event = name();
    }

    @Override
    public String event() {
        return event;
    }
}
