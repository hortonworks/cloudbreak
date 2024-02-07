package com.sequenceiq.cloudbreak.core.flow2.stack.imdupdate.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;

public class StackInstanceMetadataUpdateResult extends CloudPlatformResult implements FlowPayload {

    @JsonCreator
    public StackInstanceMetadataUpdateResult(
            @JsonProperty("resourceId") Long resourceId) {
        super(resourceId);
    }

    @Override
    public String toString() {
        return "FreeIpaInstanceMetadataUpdateResult{" +
                super.toString() +
                '}';
    }
}
