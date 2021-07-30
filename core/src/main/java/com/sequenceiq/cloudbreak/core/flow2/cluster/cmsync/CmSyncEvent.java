package com.sequenceiq.cloudbreak.core.flow2.cluster.cmsync;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.CmSyncResult;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum CmSyncEvent implements FlowEvent {
    CM_SYNC_TRIGGER_EVENT,
    CM_SYNC_FINISHED_EVENT(EventSelectorUtil.selector(CmSyncResult.class)),
    CM_SYNC_FINISHED_FAILURE_EVENT(EventSelectorUtil.failureSelector(CmSyncResult.class)),

    CM_SYNC_FINALIZED_EVENT,
    CM_SYNC_FAILURE_EVENT,
    CM_SYNC_FAIL_HANDLED_EVENT;

    private final String event;

    CmSyncEvent(String event) {
        this.event = event;
    }

    CmSyncEvent() {
        this.event = name();
    }

    @Override
    public String event() {
        return event;
    }
}
