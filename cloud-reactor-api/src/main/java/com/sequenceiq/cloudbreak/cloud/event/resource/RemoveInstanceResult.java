package com.sequenceiq.cloudbreak.cloud.event.resource;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.event.InstancePayload;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;

public class RemoveInstanceResult extends CloudPlatformResult implements InstancePayload, FlowPayload {

    private final List<CloudInstance> instances;

    public RemoveInstanceResult(DownscaleStackResult result, Long resourceId, List<CloudInstance> instances) {
        super(resourceId);
        this.instances = instances;
        init(result.getStatus(), result.getStatusReason(), result.getErrorDetails());
    }

    @JsonCreator
    public RemoveInstanceResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("instances") List<CloudInstance> instances) {
        super(statusReason, errorDetails, resourceId);
        this.instances = instances;
    }

    /**
     * Need this for Jackson serialization
     */
    private List<CloudInstance> getInstances() {
        return instances;
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
