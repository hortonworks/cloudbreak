package com.sequenceiq.cloudbreak.cloud.context;

import java.util.Objects;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;

/**
 * Context object to store the credentials and the cached Cloud Platfrom client object. The Cloud provider client objects are
 * stored in the {@link DynamicModel} and must be thread-safe.
 */
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AuthenticatedContext that = (AuthenticatedContext) o;
        return Objects.equals(cloudContext, that.cloudContext) &&
                Objects.equals(cloudCredential, that.cloudCredential);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cloudContext, cloudCredential);
    }
}
