package com.sequenceiq.datalake.flow.create.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

public class StorageConsumptionCollectionSchedulingRequest extends SdxEvent {

    private final DetailedEnvironmentResponse detailedEnvironmentResponse;

    @JsonCreator
    public StorageConsumptionCollectionSchedulingRequest(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("detailedEnvironmentResponse") DetailedEnvironmentResponse detailedEnvironmentResponse) {
        super(sdxId, userId);
        this.detailedEnvironmentResponse = detailedEnvironmentResponse;
    }

    public static StorageConsumptionCollectionSchedulingRequest from(SdxContext context, DetailedEnvironmentResponse detailedEnvironmentResponse) {
        return new StorageConsumptionCollectionSchedulingRequest(context.getSdxId(), context.getUserId(), detailedEnvironmentResponse);
    }

    public DetailedEnvironmentResponse getDetailedEnvironmentResponse() {
        return detailedEnvironmentResponse;
    }

}
