package com.sequenceiq.cloudbreak.cloud.context;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;

/**
 * Context object to store the credentials and the cached Cloud Platfrom client object. The Cloud provider client objects are
 * stored in the {@link DynamicModel} and must be thread-safe.
 */
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
