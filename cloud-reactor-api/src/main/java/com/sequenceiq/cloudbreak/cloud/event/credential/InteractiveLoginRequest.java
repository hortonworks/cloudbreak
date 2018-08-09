package com.sequenceiq.cloudbreak.cloud.event.credential;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;

public class InteractiveLoginRequest extends CloudPlatformRequest<InteractiveLoginResult> {

    private final IdentityUser identityUser;

    public InteractiveLoginRequest(CloudContext cloudContext, ExtendedCloudCredential cloudCredential, IdentityUser identityUser) {
        super(cloudContext, cloudCredential);
        this.identityUser = identityUser;
    }

    public ExtendedCloudCredential getExtendedCloudCredential() {
        return (ExtendedCloudCredential) getCloudCredential();
    }

    public IdentityUser getIdentityUser() {
        return identityUser;
    }
}
