package com.sequenceiq.cloudbreak.cloud.event.resource;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

import reactor.rx.Promise;

public class UpscaleStackRequest<T> extends CloudStackRequest<T> {

    private List<CloudResource> resourceList;
    private int adjustment;

    public UpscaleStackRequest(CloudContext cloudContext, CloudCredential cloudCredential, CloudStack cloudStack, List<CloudResource> resourceList,
            int adjustment, Promise<T> result) {
        super(cloudContext, cloudCredential, cloudStack, result);
        this.resourceList = resourceList;
        this.adjustment = adjustment;
    }

    public List<CloudResource> getResourceList() {
        return resourceList;
    }

    public int getAdjustment() {
        return adjustment;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("UpscaleStackRequest{");
        sb.append("resourceList=").append(resourceList);
        sb.append(", adjustment=").append(adjustment);
        sb.append('}');
        return sb.toString();
    }
}
