package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;

public class GetStackParamValidationRequest extends CloudPlatformRequest<GetStackParamValidationResult> {
    public GetStackParamValidationRequest(CloudContext cloudContext) {
        super(cloudContext, null);
    }

    //BEGIN GENERATED CODE
    @Override
    public String toString() {
        return "GetStackParamValidationRequest{" + getCloudContext().toString() + "}";
    }
    //END GENERATED CODE
}
