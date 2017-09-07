package com.sequenceiq.cloudbreak.cloud.event.instance;


import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;

public class StartInstancesResult extends CloudPlatformResult<StartInstancesRequest> {

    private final CloudContext cloudContext;

    private InstancesStatusResult results;

    public StartInstancesResult(StartInstancesRequest request, CloudContext cloudContext, InstancesStatusResult results) {
        super(request);
        this.cloudContext = cloudContext;
        this.results = results;
    }

    public StartInstancesResult(String statusReason, Exception errorDetails, StartInstancesRequest request) {
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
