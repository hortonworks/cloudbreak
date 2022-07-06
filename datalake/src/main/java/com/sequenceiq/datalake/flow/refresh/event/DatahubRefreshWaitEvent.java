package com.sequenceiq.datalake.flow.refresh.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatahubRefreshWaitEvent extends SdxEvent {
    @JsonCreator
    public DatahubRefreshWaitEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(sdxId, userId);
    }

}
