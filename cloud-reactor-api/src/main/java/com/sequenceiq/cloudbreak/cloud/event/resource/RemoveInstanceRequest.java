package com.sequenceiq.cloudbreak.cloud.event.resource;

import java.util.Collections;
import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

public class RemoveInstanceRequest<T> extends DownscaleStackRequest<T> {

    public RemoveInstanceRequest(CloudContext cloudContext, CloudCredential cloudCredential, CloudStack cloudStack, List<CloudResource> cloudResources,
            CloudInstance instance) {
        super(cloudContext, cloudCredential, cloudStack, cloudResources, Collections.singletonList(instance));
    }
}
