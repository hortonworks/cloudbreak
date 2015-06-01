package com.sequenceiq.cloudbreak.cloud.event.context;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DynamicModel;

public class AuthenticatedContext extends DynamicModel {

    private StackContext stackContext;

    private CloudCredential cloudCredential;

    public AuthenticatedContext(StackContext stackContext, CloudCredential cloudCredential) {
        this.stackContext = stackContext;
        this.cloudCredential = cloudCredential;
    }

    public StackContext getStackContext() {
        return stackContext;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }


}
