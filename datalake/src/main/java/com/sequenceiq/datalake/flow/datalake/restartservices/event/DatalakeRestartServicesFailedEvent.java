package com.sequenceiq.datalake.flow.datalake.restartservices.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxFailedEvent;

public class DatalakeRestartServicesFailedEvent extends SdxFailedEvent {

    @JsonCreator
    public DatalakeRestartServicesFailedEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("exception") Exception exception) {
        super(sdxId, userId, exception);
    }
}
