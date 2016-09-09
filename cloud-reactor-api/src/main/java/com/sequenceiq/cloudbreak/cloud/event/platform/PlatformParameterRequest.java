package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

public class PlatformParameterRequest extends CloudPlatformRequest<PlatformParameterResult> {

    public PlatformParameterRequest(CloudContext cloudContext, CloudCredential cloudCredential) {
        super(cloudContext, cloudCredential);
    }

    //BEGIN GENERATED CODE
    @Override
    public String toString() {
        return "PlatformParameterRequest{}";
    }
    //END GENERATED CODE
}
