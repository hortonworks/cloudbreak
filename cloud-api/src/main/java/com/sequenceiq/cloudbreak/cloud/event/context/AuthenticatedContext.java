package com.sequenceiq.cloudbreak.cloud.event.context;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DynamicModel;

public class AuthenticatedContext extends DynamicModel {

    private final CloudContext cloudContext;
    private final CloudCredential cloudCredential;

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
