package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

public class GetPlatformVariantsRequest extends CloudPlatformRequest<GetPlatformVariantsResult> {

    public GetPlatformVariantsRequest() {
        super(null, null);
    }

    public GetPlatformVariantsRequest(CloudContext cloudContext, CloudCredential cloudCredential) {
        super(cloudContext, cloudCredential);
    }

    //BEGIN GENERATED CODE
    @Override
    public String toString() {
        return "GetPlatformVariantsRequest{}";
    }
    //END GENERATED CODE
}
