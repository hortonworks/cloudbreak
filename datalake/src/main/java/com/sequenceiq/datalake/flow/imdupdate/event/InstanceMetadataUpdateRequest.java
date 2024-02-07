package com.sequenceiq.datalake.flow.imdupdate.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateType;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class InstanceMetadataUpdateRequest extends SdxEvent {

    private final InstanceMetadataUpdateType updateType;

    @JsonCreator
    public InstanceMetadataUpdateRequest(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("updateType") InstanceMetadataUpdateType updateType) {
        super(sdxId, userId);
        this.updateType = updateType;
    }

    public static InstanceMetadataUpdateRequest from(SdxContext context, InstanceMetadataUpdateType updateType) {
        return new InstanceMetadataUpdateRequest(context.getSdxId(), context.getUserId(), updateType);
    }

    public InstanceMetadataUpdateType getUpdateType() {
        return updateType;
    }

    @Override
    public String toString() {
        return "InstanceMetadataUpdateRequest{" +
                "updateType=" + updateType +
                "} " + super.toString();
    }

}