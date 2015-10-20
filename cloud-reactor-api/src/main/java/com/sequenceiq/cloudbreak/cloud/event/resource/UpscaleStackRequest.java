package com.sequenceiq.cloudbreak.cloud.event.resource;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

public class UpscaleStackRequest<T> extends CloudStackRequest<T> {

    private List<CloudResource> resourceList;

    public UpscaleStackRequest(CloudContext cloudContext, CloudCredential cloudCredential, CloudStack stack, List<CloudResource> resourceList) {
        super(cloudContext, cloudCredential, stack);
        this.resourceList = resourceList;
    }

    public List<CloudResource> getResourceList() {
        return resourceList;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("UpscaleStackRequest{");
        sb.append("resourceList=").append(resourceList);
        sb.append('}');
        return sb.toString();
    }
}
