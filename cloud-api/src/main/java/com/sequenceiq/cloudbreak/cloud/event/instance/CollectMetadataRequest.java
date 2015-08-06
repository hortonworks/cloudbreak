package com.sequenceiq.cloudbreak.cloud.event.instance;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;

import reactor.rx.Promise;

public class CollectMetadataRequest<T> extends CloudPlatformRequest<T> {

    private List<CloudResource> cloudResource;

    private List<InstanceTemplate> vms;

    public CollectMetadataRequest(CloudContext cloudContext, CloudCredential cloudCredential, List<CloudResource> cloudResource, List<InstanceTemplate> vms,
            Promise<T> result) {
        super(cloudContext, cloudCredential, result);
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
