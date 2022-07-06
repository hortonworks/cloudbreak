package com.sequenceiq.datalake.flow.datalake.cmsync.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxCmSyncWaitEvent extends SdxEvent {

    @JsonCreator
    public SdxCmSyncWaitEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("sdxName") String sdxName,
            @JsonProperty("userId") String userId) {

        super(selector, sdxId, sdxName, userId);
    }

    public SdxCmSyncWaitEvent(SdxContext context) {
        super(context);
    }
}
