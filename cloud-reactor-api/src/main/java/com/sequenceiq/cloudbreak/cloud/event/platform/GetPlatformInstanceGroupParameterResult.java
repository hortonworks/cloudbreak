package com.sequenceiq.cloudbreak.cloud.event.platform;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.InstanceGroupParameterResponse;

public class GetPlatformInstanceGroupParameterResult extends CloudPlatformResult {

    private Map<String, InstanceGroupParameterResponse> instanceGroupParameterResponses = new HashMap<>();

    public GetPlatformInstanceGroupParameterResult(Long resourceId,
            Map<String, InstanceGroupParameterResponse> instanceGroupParameterResponses) {
        super(resourceId);
        this.instanceGroupParameterResponses = instanceGroupParameterResponses;
    }

    public GetPlatformInstanceGroupParameterResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }

    public Map<String, InstanceGroupParameterResponse> getInstanceGroupParameterResponses() {
        return instanceGroupParameterResponses;
    }
}
