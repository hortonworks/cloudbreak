package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.VmRecommendations;

public class GetVirtualMachineRecommendationResponse extends CloudPlatformResult {

    private VmRecommendations recommendations;

    public GetVirtualMachineRecommendationResponse(Long resourceId, VmRecommendations recommendations) {
        super(resourceId);
        this.recommendations = recommendations;
    }

    public GetVirtualMachineRecommendationResponse(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }

    public VmRecommendations getRecommendations() {
        return recommendations;
    }
}
