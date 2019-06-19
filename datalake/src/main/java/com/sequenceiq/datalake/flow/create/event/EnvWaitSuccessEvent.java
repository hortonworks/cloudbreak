package com.sequenceiq.datalake.flow.create.event;

import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

public class EnvWaitSuccessEvent extends SdxEvent {

    private DetailedEnvironmentResponse detailedEnvironmentResponse;

    public EnvWaitSuccessEvent(Long sdxId, DetailedEnvironmentResponse detailedEnvironmentResponse) {
        super(sdxId);
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
