package com.sequenceiq.freeipa.flow.freeipa.imdupdate.event;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.CloudStackRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateType;

public class FreeIpaInstanceMetadataUpdateRequest extends CloudStackRequest<FreeIpaInstanceMetadataUpdateResult> {

    private final InstanceMetadataUpdateType updateType;

    public FreeIpaInstanceMetadataUpdateRequest(
            @JsonProperty("cloudContext") CloudContext cloudContext,
            @JsonProperty("cloudCredential") CloudCredential cloudCredential,
            @JsonProperty("stack") CloudStack stack,
            @JsonProperty("updateType") InstanceMetadataUpdateType updateType) {
        super(cloudContext, cloudCredential, stack);
        this.updateType = updateType;
    }

    public InstanceMetadataUpdateType getUpdateType() {
        return updateType;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", FreeIpaInstanceMetadataUpdateRequest.class.getSimpleName() + "[", "]")
                .add("updateType=" + updateType)
                .add(super.toString())
                .toString();
    }
}
