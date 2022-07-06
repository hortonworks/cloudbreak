package com.sequenceiq.datalake.flow.refresh.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.refresh.DatahubRefreshFlowEvent;

public class DatahubRefreshStartEvent extends SdxEvent {

    @JsonCreator
    public DatahubRefreshStartEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("sdxName") String sdxName,
            @JsonProperty("userId") String userId) {
        super(DatahubRefreshFlowEvent.DATAHUB_REFRESH_START_EVENT.event(), sdxId, sdxName, userId);
    }
}
