package com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata;

import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.CloudStackRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.common.api.type.InstanceGroupType;

public class UserDataUpdateOnProviderRequest extends CloudStackRequest<UserDataUpdateOnProviderResult> {
    private final List<CloudResource> cloudResources;

    private final Map<InstanceGroupType, String> userData;

    public UserDataUpdateOnProviderRequest(CloudContext cloudContext, CloudCredential cloudCredential, CloudStack cloudStack, List<CloudResource> cloudResources,
            Map<InstanceGroupType, String> userData) {
        super(cloudContext, cloudCredential, cloudStack);
        this.cloudResources = cloudResources;
        this.userData = userData;
    }

    public List<CloudResource> getCloudResources() {
        return cloudResources;
    }

    public Map<InstanceGroupType, String> getUserData() {
        return userData;
    }

    @Override
    public String toString() {
        return "UserDataUpdateOnProviderRequest{" +
                "cloudResources=" + cloudResources +
                "} " + super.toString();
    }
}
