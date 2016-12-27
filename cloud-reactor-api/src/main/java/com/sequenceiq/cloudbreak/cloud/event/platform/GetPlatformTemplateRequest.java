package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

public class GetPlatformTemplateRequest extends CloudPlatformRequest<GetPlatformTemplateResult> {

    public GetPlatformTemplateRequest(CloudContext cloudContext, CloudCredential cloudCredential) {
        super(cloudContext, cloudCredential);
    }

    @Override
    public String toString() {
        return "GetPlatformTemplateRequest{}";
    }

}
