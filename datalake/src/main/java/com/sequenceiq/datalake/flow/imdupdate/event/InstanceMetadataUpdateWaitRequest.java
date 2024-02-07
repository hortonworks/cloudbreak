package com.sequenceiq.datalake.flow.imdupdate.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class InstanceMetadataUpdateWaitRequest extends SdxEvent {

    @JsonCreator
    public InstanceMetadataUpdateWaitRequest(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(sdxId, userId);
    }

    public static InstanceMetadataUpdateWaitRequest from(SdxContext context) {
        return new InstanceMetadataUpdateWaitRequest(context.getSdxId(), context.getUserId());
    }

    @Override
    public String toString() {
        return "InstanceMetadataUpdateWaitRequest{} " + super.toString();
    }

}