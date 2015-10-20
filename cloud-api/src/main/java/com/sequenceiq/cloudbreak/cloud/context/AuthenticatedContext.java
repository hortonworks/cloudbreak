package com.sequenceiq.cloudbreak.cloud.context;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;

public class AuthenticatedContext extends DynamicModel {

    private CloudContext cloudContext;
    private CloudCredential cloudCredential;

    public AuthenticatedContext(CloudContext cloudContext, CloudCredential cloudCredential) {
        this.cloudContext = cloudContext;
        this.cloudCredential = cloudCredential;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }


}
