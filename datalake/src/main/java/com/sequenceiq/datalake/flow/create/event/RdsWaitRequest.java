package com.sequenceiq.datalake.flow.create.event;

import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

public class RdsWaitRequest extends SdxEvent {

    private DetailedEnvironmentResponse detailedEnvironmentResponse;

    public RdsWaitRequest(Long sdxId, String userId, String requestId, DetailedEnvironmentResponse detailedEnvironmentResponse) {
        super(sdxId, userId, requestId);
        this.detailedEnvironmentResponse = detailedEnvironmentResponse;
    }

    public RdsWaitRequest(SdxContext context, DetailedEnvironmentResponse detailedEnvironmentResponse) {
        super(context);
        this.detailedEnvironmentResponse = detailedEnvironmentResponse;
    }

    @Override
    public String selector() {
        return "RdsWaitRequest";
    }

    public DetailedEnvironmentResponse getDetailedEnvironmentResponse() {
        return detailedEnvironmentResponse;
    }
}
