package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.VmRecommendations;

public class GetVirtualMachineRecommendationResponse extends CloudPlatformResult<GetVirtualMachineRecommendtaionRequest> {

    private VmRecommendations recommendations;

    public GetVirtualMachineRecommendationResponse(GetVirtualMachineRecommendtaionRequest request, VmRecommendations recommendations) {
        super(request);
        this.recommendations = recommendations;
    }

    public GetVirtualMachineRecommendationResponse(String statusReason, Exception errorDetails, GetVirtualMachineRecommendtaionRequest request) {
        super(statusReason, errorDetails, request);
    }

    public VmRecommendations getRecommendations() {
        return recommendations;
    }
}
