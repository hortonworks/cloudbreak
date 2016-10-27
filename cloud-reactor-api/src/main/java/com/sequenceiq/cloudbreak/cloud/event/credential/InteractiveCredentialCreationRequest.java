package com.sequenceiq.cloudbreak.cloud.event.credential;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;

/**
 * Created by perdos on 9/23/16.
 */
public class InteractiveCredentialCreationRequest extends CloudPlatformRequest {

    private ExtendedCloudCredential extendedCloudCredential;

    public InteractiveCredentialCreationRequest(CloudContext cloudContext, CloudCredential cloudCredential, ExtendedCloudCredential extendedCloudCredential) {
        super(cloudContext, cloudCredential);
        this.extendedCloudCredential = extendedCloudCredential;
    }

    public ExtendedCloudCredential getExtendedCloudCredential() {
        return extendedCloudCredential;
    }
}
