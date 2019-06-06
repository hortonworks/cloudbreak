package com.sequenceiq.cloudbreak.cloud.event.resource;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.event.InstancePayload;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;

public class RemoveInstanceResult extends CloudPlatformResult implements InstancePayload {

    private List<CloudInstance> instances;

    public RemoveInstanceResult(DownscaleStackResult result, Long resourceId, List<CloudInstance> instances) {
        super(resourceId);
        this.instances = instances;
        init(result.getStatus(), result.getStatusReason(), result.getErrorDetails());
    }

    public RemoveInstanceResult(String statusReason, Exception errorDetails, Long resourceId, List<CloudInstance> instances) {
        super(statusReason, errorDetails, resourceId);
        this.instances = instances;
    }

    @Override
    public Set<String> getInstanceIds() {
        if (CollectionUtils.isEmpty(instances)) {
            return null;
        } else {
            return instances.stream().map(CloudInstance::getInstanceId).collect(Collectors.toSet());
        }
    }
}
