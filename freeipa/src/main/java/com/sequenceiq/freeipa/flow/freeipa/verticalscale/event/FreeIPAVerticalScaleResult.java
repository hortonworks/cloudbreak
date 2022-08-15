package com.sequenceiq.freeipa.flow.freeipa.verticalscale.event;

import java.util.ArrayList;
import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.FreeIPAVerticalScaleRequest;

public class FreeIPAVerticalScaleResult extends CloudPlatformResult {

    private final ResourceStatus resourceStatus;

    private final List<CloudResourceStatus> results;

    private final FreeIPAVerticalScaleRequest freeIPAVerticalScaleRequest;

    public FreeIPAVerticalScaleResult(Long resourceId, ResourceStatus resourceStatus, List<CloudResourceStatus> results,
            FreeIPAVerticalScaleRequest freeIPAVerticalScaleRequest) {
        super(resourceId);
        this.resourceStatus = resourceStatus;
        this.results = results;
        this.freeIPAVerticalScaleRequest = freeIPAVerticalScaleRequest;
    }

    public FreeIPAVerticalScaleResult(String statusReason, Exception errorDetails, Long resourceId,
            FreeIPAVerticalScaleRequest freeIPAVerticalScaleRequest) {
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

    public FreeIPAVerticalScaleRequest getFreeIPAVerticalScaleV1Request() {
        return freeIPAVerticalScaleRequest;
    }
}
