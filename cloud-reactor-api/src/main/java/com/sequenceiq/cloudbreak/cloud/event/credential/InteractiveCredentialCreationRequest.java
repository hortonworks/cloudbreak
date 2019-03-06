package com.sequenceiq.cloudbreak.cloud.event.credential;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;

public class InteractiveCredentialCreationRequest extends CloudPlatformRequest<InteractiveCredentialCreationStatus> {

    private final ExtendedCloudCredential extendedCloudCredential;

    public InteractiveCredentialCreationRequest(CloudContext cloudContext, ExtendedCloudCredential extendedCloudCredential) {
        super(cloudContext, extendedCloudCredential);
        this.extendedCloudCredential = extendedCloudCredential;
    }

    public ExtendedCloudCredential getExtendedCloudCredential() {
        return extendedCloudCredential;
    }
}
