package com.sequenceiq.cloudbreak.cloud.event.platform;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.InstanceGroupParameterResponse;

public class GetPlatformInstanceGroupParameterResult extends CloudPlatformResult<CloudPlatformRequest<?>> {

    private Map<String, InstanceGroupParameterResponse> instanceGroupParameterResponses = new HashMap<>();

    public GetPlatformInstanceGroupParameterResult(CloudPlatformRequest<?> request,
            Map<String, InstanceGroupParameterResponse> instanceGroupParameterResponses) {
        super(request);
        this.instanceGroupParameterResponses = instanceGroupParameterResponses;
    }

    public GetPlatformInstanceGroupParameterResult(String statusReason, Exception errorDetails, CloudPlatformRequest<?> request) {
        super(statusReason, errorDetails, request);
    }

    public Map<String, InstanceGroupParameterResponse> getInstanceGroupParameterResponses() {
        return instanceGroupParameterResponses;
    }
}
