package com.sequenceiq.datalake.flow.datalake.restartservices.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeRestartServicesWaitEvent extends SdxEvent {
    @JsonCreator
    public DatalakeRestartServicesWaitEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(sdxId, userId);
    }

}
