package com.sequenceiq.datalake.flow.create.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

public class EnvWaitSuccessEvent extends SdxEvent {

    private final DetailedEnvironmentResponse detailedEnvironmentResponse;

    @JsonCreator
    public EnvWaitSuccessEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("detailedEnvironmentResponse") DetailedEnvironmentResponse detailedEnvironmentResponse) {
        super(sdxId, userId);
        this.detailedEnvironmentResponse = detailedEnvironmentResponse;
    }

    @Override
    public String selector() {
        return "EnvWaitSuccessEvent";
    }

    public DetailedEnvironmentResponse getDetailedEnvironmentResponse() {
        return detailedEnvironmentResponse;
    }
}
