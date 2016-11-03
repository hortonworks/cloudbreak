package com.sequenceiq.cloudbreak.cloud.event.resource;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

import java.util.List;

public class RemoveInstanceRequest<T> extends DownscaleStackRequest<T> {

    public RemoveInstanceRequest(CloudContext cloudContext, CloudCredential cloudCredential, CloudStack cloudStack, List<CloudResource> cloudResources,
            List<CloudInstance> instances) {
        super(cloudContext, cloudCredential, cloudStack, cloudResources, instances);
    }
}
