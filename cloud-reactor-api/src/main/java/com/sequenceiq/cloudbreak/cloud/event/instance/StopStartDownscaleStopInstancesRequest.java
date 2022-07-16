package com.sequenceiq.cloudbreak.cloud.event.instance;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.CloudStackRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

public class StopStartDownscaleStopInstancesRequest extends CloudStackRequest<StopStartDownscaleStopInstancesResult> {

    private final List<CloudInstance> cloudInstancesToStop;

    @JsonCreator
    public StopStartDownscaleStopInstancesRequest(
            @JsonProperty("cloudContext") CloudContext cloudContext,
            @JsonProperty("cloudCredential") CloudCredential cloudCredential,
            @JsonProperty("cloudStack") CloudStack cloudStack,
            @JsonProperty("cloudInstancesToStop") List<CloudInstance> cloudInstancesToStop) {
        super(cloudContext, cloudCredential, cloudStack);
        this.cloudInstancesToStop = cloudInstancesToStop;
    }

    public List<CloudInstance> getCloudInstancesToStop() {
        return cloudInstancesToStop;
    }

    @Override
    public String toString() {
        return "StopStartDownscaleStopInstancesRequest{" +
                "cloudInstancesToStop=" + cloudInstancesToStop +
                '}';
    }
}
