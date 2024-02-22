package com.sequenceiq.freeipa.flow.freeipa.imdupdate.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;

public class FreeIpaInstanceMetadataUpdateResult extends CloudPlatformResult implements FlowPayload {

    @JsonCreator
    public FreeIpaInstanceMetadataUpdateResult(
            @JsonProperty("resourceId") Long resourceId) {
        super(resourceId);
    }

    @Override
    public String toString() {
        return "FreeIpaInstanceMetadataUpdateResult{" + super.toString() + '}';
    }
}
