package com.sequenceiq.freeipa.flow.stack.update.event;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.CloudStackRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

public class UserDataUpdateOnProviderRequest extends CloudStackRequest<UserDataUpdateOnProviderResult> {
    private final List<CloudResource> cloudResources;

    private final String userData;

    public UserDataUpdateOnProviderRequest(CloudContext cloudContext, CloudCredential cloudCredential, CloudStack cloudStack, List<CloudResource> cloudResources,
            String userData) {
        super(cloudContext, cloudCredential, cloudStack);
        this.cloudResources = cloudResources;
        this.userData = userData;
    }

    public List<CloudResource> getCloudResources() {
        return cloudResources;
    }

    public String getUserData() {
        return userData;
    }

    @Override
    public String toString() {
        return "UserDataUpdateOnProviderRequest{" +
                "cloudResources=" + cloudResources +
                ", userData='" + userData + '\'' +
                "} " + super.toString();
    }
}
