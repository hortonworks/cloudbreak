package com.sequenceiq.cloudbreak.core.flow2.cluster.cmsync;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.CmSyncResult;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum CmSyncEvent implements FlowEvent {
    CM_SYNC_EVENT("CM_SYNC_TRIGGER_EVENT"),
    CM_SYNC_FINISHED_EVENT(EventSelectorUtil.selector(CmSyncResult.class)),
    CM_SYNC_FINISHED_FAILURE_EVENT(EventSelectorUtil.failureSelector(CmSyncResult.class)),

    FINALIZED_EVENT("CMSYNCFINALIZEDEVENT"),
    FAILURE_EVENT("CMSYNCFAILUREEVENT"),
    FAIL_HANDLED_EVENT("CMSYNCFAILHANDLEDEVENT");

    private final String event;

    CmSyncEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
