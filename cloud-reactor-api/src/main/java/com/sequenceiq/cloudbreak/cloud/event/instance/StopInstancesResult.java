package com.sequenceiq.cloudbreak.cloud.event.instance;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.event.Payload;

public class StopInstancesResult extends CloudPlatformResult<StopInstancesRequest> implements Payload {

    private CloudContext cloudContext;
    private InstancesStatusResult results;

    public StopInstancesResult(StopInstancesRequest request, CloudContext cloudContext, InstancesStatusResult results) {
        super(request);
        this.cloudContext = cloudContext;
        this.results = results;
    }

    public StopInstancesResult(String statusReason, Exception errorDetails, StopInstancesRequest request) {
        super(statusReason, errorDetails, request);
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
