package com.sequenceiq.freeipa.flow.freeipa.upscale.event;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;

public class UpscaleStackResult extends CloudPlatformResult implements FlowPayload {

    private final ResourceStatus resourceStatus;

    private final List<CloudResourceStatus> results;

    @JsonCreator
    public UpscaleStackResult(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceStatus") ResourceStatus resourceStatus,
            @JsonProperty("results") List<CloudResourceStatus> results) {

        super(resourceId);
        this.resourceStatus = resourceStatus;
        this.results = results;
    }

    public UpscaleStackResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
        resourceStatus = ResourceStatus.FAILED;
        results = new ArrayList<>();
    }

    public List<CloudResourceStatus> getResults() {
        return results;
    }

    public ResourceStatus getResourceStatus() {
        return resourceStatus;
    }

    public boolean isFailed() {
        return resourceStatus == ResourceStatus.FAILED;
    }

    @Override
    public String toString() {
        return "UpscaleStackResult{" +
                "resourceStatus=" + resourceStatus +
                ", results=" + results +
                "} " + super.toString();
    }
}