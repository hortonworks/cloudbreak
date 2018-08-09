package com.sequenceiq.cloudbreak.cloud.event.credential;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;

/**
 * Created by perdos on 9/23/16.
 */
public class InteractiveCredentialCreationRequest extends CloudPlatformRequest {

    private final ExtendedCloudCredential extendedCloudCredential;

    private final IdentityUser identityUser;

    public InteractiveCredentialCreationRequest(CloudContext cloudContext, ExtendedCloudCredential extendedCloudCredential, IdentityUser identityUser) {
        super(cloudContext, extendedCloudCredential);
        this.extendedCloudCredential = extendedCloudCredential;
        this.identityUser = identityUser;
    }

    public ExtendedCloudCredential getExtendedCloudCredential() {
        return extendedCloudCredential;
    }

    public IdentityUser getIdentityUser() {
        return identityUser;
    }
}
