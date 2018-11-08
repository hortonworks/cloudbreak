package com.sequenceiq.cloudbreak.cloud.event.credential;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

public class InitCodeGrantFlowRequest  extends CloudPlatformRequest<InitCodeGrantFlowResponse> {

    public InitCodeGrantFlowRequest(CloudContext cloudContext, CloudCredential cloudCredential) {
        super(cloudContext, cloudCredential);
    }
}
