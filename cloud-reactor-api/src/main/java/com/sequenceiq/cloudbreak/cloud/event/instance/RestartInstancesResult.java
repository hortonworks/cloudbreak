package com.sequenceiq.cloudbreak.cloud.event.instance;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;

public class RestartInstancesResult extends CloudPlatformResult implements FlowPayload {

    private final InstancesStatusResult results;

    private final List<String> instanceIds;

    public RestartInstancesResult(Long resourceId, InstancesStatusResult results, List<String> instanceIds) {
        super(resourceId);
        this.results = results;
        this.instanceIds = instanceIds;
    }

    @JsonCreator
    public RestartInstancesResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("instanceIds") List<String> instanceIds) {
        super(statusReason, errorDetails, resourceId);
        this.instanceIds = instanceIds;
        this.results = null;
    }

    public InstancesStatusResult getResults() {
        return results;
    }

    public List<String> getInstanceIds() {
        return instanceIds;
    }
}
