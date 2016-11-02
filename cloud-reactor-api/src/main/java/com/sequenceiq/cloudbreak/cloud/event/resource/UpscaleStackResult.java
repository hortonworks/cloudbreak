package com.sequenceiq.cloudbreak.cloud.event.resource;

import java.util.ArrayList;
import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;

public class UpscaleStackResult extends CloudPlatformResult<CloudPlatformRequest> {

    private ResourceStatus resourceStatus;

    private List<CloudResourceStatus> results;

    public UpscaleStackResult(CloudPlatformRequest<?> request, ResourceStatus resourceStatus, List<CloudResourceStatus> results) {
        super(request);
        this.resourceStatus = resourceStatus;
        this.results = results;
    }

    public UpscaleStackResult(String statusReason, Exception errorDetails, CloudPlatformRequest<?> request) {
        super(statusReason, errorDetails, request);
        this.resourceStatus = ResourceStatus.FAILED;
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

}
