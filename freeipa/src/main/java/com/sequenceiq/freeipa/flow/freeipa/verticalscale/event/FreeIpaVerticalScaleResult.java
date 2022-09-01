package com.sequenceiq.freeipa.flow.freeipa.verticalscale.event;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleRequest;

public class FreeIpaVerticalScaleResult extends CloudPlatformResult implements FlowPayload {

    private final ResourceStatus resourceStatus;

    private final List<CloudResourceStatus> results;

    private final VerticalScaleRequest freeIPAVerticalScaleRequest;

    @JsonCreator
    public FreeIpaVerticalScaleResult(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceStatus") ResourceStatus resourceStatus,
            @JsonProperty("results") List<CloudResourceStatus> results,
            @JsonProperty("freeIPAVerticalScaleRequest") VerticalScaleRequest freeIPAVerticalScaleRequest) {
        super(resourceId);
        this.resourceStatus = resourceStatus;
        this.results = results;
        this.freeIPAVerticalScaleRequest = freeIPAVerticalScaleRequest;
    }

    public FreeIpaVerticalScaleResult(String statusReason, Exception errorDetails, Long resourceId,
            VerticalScaleRequest freeIPAVerticalScaleRequest) {
        super(statusReason, errorDetails, resourceId);
        this.resourceStatus = ResourceStatus.FAILED;
        this.freeIPAVerticalScaleRequest = freeIPAVerticalScaleRequest;
        this.results = new ArrayList<>();
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

    public VerticalScaleRequest getFreeIPAVerticalScaleRequest() {
        return freeIPAVerticalScaleRequest;
    }

    @Override
    public String toString() {
        return super.toString() + "FreeIpaVerticalScaleResult{" +
                "resourceStatus=" + resourceStatus +
                ", results=" + results +
                ", freeIPAVerticalScaleRequest=" + freeIPAVerticalScaleRequest +
                '}';
    }
}
