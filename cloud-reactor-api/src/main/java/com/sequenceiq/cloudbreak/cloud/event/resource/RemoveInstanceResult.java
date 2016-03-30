package com.sequenceiq.cloudbreak.cloud.event.resource;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;

public class RemoveInstanceResult extends CloudPlatformResult<RemoveInstanceRequest> {

    public RemoveInstanceResult(DownscaleStackResult result, RemoveInstanceRequest<?> request) {
        init(result.getStatus(), result.getStatusReason(), result.getErrorDetails(), request);
    }

    public RemoveInstanceResult(String statusReason, Exception errorDetails, RemoveInstanceRequest request) {
        super(statusReason, errorDetails, request);
    }

    public CloudInstance getCloudInstance() {
        List<CloudInstance> instances = getRequest().getInstances();
        return instances.isEmpty() ? null : instances.get(0);
    }
}
