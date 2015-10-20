package com.sequenceiq.cloudbreak.cloud.event.resource;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

public class UpdateStackRequest<T> extends CloudStackRequest<T> {

    private List<CloudResource> resourceList;

    public UpdateStackRequest(CloudContext cloudContext, CloudCredential cloudCredential, CloudStack cloudStack, List<CloudResource> resourceList) {
        super(cloudContext, cloudCredential, cloudStack);
        this.resourceList = resourceList;
    }

    public List<CloudResource> getResourceList() {
        return resourceList;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("UpdateStackRequest{");
        sb.append("resourceList=").append(resourceList);
        sb.append('}');
        return sb.toString();
    }
}
