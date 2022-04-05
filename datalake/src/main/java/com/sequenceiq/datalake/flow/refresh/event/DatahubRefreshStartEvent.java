package com.sequenceiq.datalake.flow.refresh.event;

import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.refresh.DatahubRefreshFlowEvent;

public class DatahubRefreshStartEvent extends SdxEvent {
    public DatahubRefreshStartEvent(Long sdxId, String sdxName, String userId) {
        super(DatahubRefreshFlowEvent.DATAHUB_REFRESH_START_EVENT.event(), sdxId, sdxName, userId);
    }
}
