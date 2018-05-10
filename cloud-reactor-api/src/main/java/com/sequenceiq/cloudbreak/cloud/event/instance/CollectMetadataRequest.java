package com.sequenceiq.cloudbreak.cloud.event.instance;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

public class CollectMetadataRequest extends CloudPlatformRequest<CollectMetadataResult> {

    private final List<CloudResource> cloudResource;

    private final List<CloudInstance> knownVms;

    private final List<CloudInstance> vms;

    public CollectMetadataRequest(CloudContext cloudContext, CloudCredential cloudCredential, List<CloudResource> cloudResource,
            List<CloudInstance> vms, List<CloudInstance> knownVms) {
        super(cloudContext, cloudCredential);
        this.cloudResource = cloudResource;
        this.vms = vms;
        this.knownVms = knownVms;
    }

    public List<CloudResource> getCloudResource() {
        return cloudResource;
    }

    public List<CloudInstance> getVms() {
        return vms;
    }

    public List<CloudInstance> getKnownVms() {
        return knownVms;
    }

    //BEGIN GENERATED CODE
    @Override
    public String toString() {
        return "CollectMetadataRequest{" +
                ", cloudResource=" + cloudResource +
                ", vms=" + vms +
                ", knownVms=" + knownVms +
                '}';
    }
    //END GENERATED CODE

}
