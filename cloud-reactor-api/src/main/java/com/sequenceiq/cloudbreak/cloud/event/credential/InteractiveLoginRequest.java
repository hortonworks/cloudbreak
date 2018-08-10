package com.sequenceiq.cloudbreak.cloud.event.credential;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;

public class InteractiveLoginRequest extends CloudPlatformRequest<InteractiveLoginResult> {

    public InteractiveLoginRequest(CloudContext cloudContext, ExtendedCloudCredential cloudCredential) {
        super(cloudContext, cloudCredential);
    }

    public ExtendedCloudCredential getExtendedCloudCredential() {
        return (ExtendedCloudCredential) getCloudCredential();
    }
}
