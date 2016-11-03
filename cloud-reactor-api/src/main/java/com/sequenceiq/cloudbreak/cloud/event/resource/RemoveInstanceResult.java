package com.sequenceiq.cloudbreak.cloud.event.resource;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.event.InstancePayload;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RemoveInstanceResult extends CloudPlatformResult<RemoveInstanceRequest> implements InstancePayload {

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

    @Override
    public Set<String> getInstanceIds() {
        if (getCloudInstance() == null) {
            return null;
        } else {
            List<CloudInstance> instances = getRequest().getInstances();
            return instances.stream().map(CloudInstance::getInstanceId).collect(Collectors.toSet());
        }
    }
}
