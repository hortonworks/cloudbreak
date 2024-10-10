package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;

public class GetCdpPlatformRegionsRequest extends CloudPlatformRequest<GetCdpPlatformRegionsResult> {

    public GetCdpPlatformRegionsRequest(CloudContext cloudContext) {
        super(cloudContext, null);
    }

    //BEGIN GENERATED CODE
    @Override
    public String toString() {
        return "GetCdpPlatformRegionsRequest{}";
    }
    //END GENERATED CODE
}
