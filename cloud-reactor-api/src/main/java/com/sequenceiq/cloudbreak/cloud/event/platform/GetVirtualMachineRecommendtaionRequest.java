package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;

public class GetVirtualMachineRecommendtaionRequest extends CloudPlatformRequest<GetVirtualMachineRecommendationResponse> {

    private final String cloudPlatform;

    public GetVirtualMachineRecommendtaionRequest(String cloudPlatform) {
        super(null, null);
        this.cloudPlatform = cloudPlatform;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    //BEGIN GENERATED CODE
    @Override
    public String toString() {
        return "GetVirtualMachineRecommendtaionRequest{ cloudPlatform: '" + cloudPlatform + "'}";
    }
    //END GENERATED CODE
}
