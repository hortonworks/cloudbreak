package com.sequenceiq.cloudbreak.cloud.event.instance;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.StackPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;

public class CollectMetadataRequest<T> extends StackPlatformRequest<T> {

    private final List<CloudResource> cloudResource;
    private final List<InstanceTemplate> vms;

    public CollectMetadataRequest(CloudContext cloudContext, CloudCredential cloudCredential, CloudStack cloudStack,
            List<CloudResource> cloudResource, List<InstanceTemplate> vms) {
        super(cloudContext, cloudCredential, cloudStack);
        this.cloudResource = cloudResource;
        this.vms = vms;
    }

    public List<CloudResource> getCloudResource() {
        return cloudResource;
    }

    public List<InstanceTemplate> getVms() {
        return vms;
    }

    //BEGIN GENERATED CODE
    @Override
    public String toString() {
        return "CollectMetadataRequest{" +
                ", cloudResource=" + cloudResource +
                ", vms=" + vms +
                '}';
    }
    //END GENERATED CODE

}
