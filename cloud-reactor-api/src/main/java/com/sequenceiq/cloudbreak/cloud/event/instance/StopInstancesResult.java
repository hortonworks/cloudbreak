package com.sequenceiq.cloudbreak.cloud.event.instance;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;

public class StopInstancesResult extends CloudPlatformResult<StopInstancesRequest> {

    private final CloudContext cloudContext;

    private InstancesStatusResult results;

    public StopInstancesResult(StopInstancesRequest request, CloudContext cloudContext, InstancesStatusResult results) {
        super(request);
        this.cloudContext = cloudContext;
        this.results = results;
    }

    public StopInstancesResult(String statusReason, Exception errorDetails, StopInstancesRequest request) {
        super(statusReason, errorDetails, request);
        cloudContext = request.getCloudContext();
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public InstancesStatusResult getResults() {
        return results;
    }

    @Override
    public Long getStackId() {
        return cloudContext.getId();
    }

}
