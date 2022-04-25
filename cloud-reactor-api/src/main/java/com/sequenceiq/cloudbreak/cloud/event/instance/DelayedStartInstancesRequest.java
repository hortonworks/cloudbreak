package com.sequenceiq.cloudbreak.cloud.event.instance;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

public class DelayedStartInstancesRequest extends StartInstancesRequest {

    private final long delayInSec;

    public DelayedStartInstancesRequest(CloudContext cloudContext, CloudCredential credential, List<CloudResource> resources,
            List<CloudInstance> cloudInstances, long delayInSec) {
        super(cloudContext, credential, resources, cloudInstances);
        this.delayInSec = delayInSec;
    }

    public long getDelayInSec() {
        return delayInSec;
    }

    @Override
    public String toString() {
        return "DelayedStartInstancesRequest{" +
                "delayInSec=" + delayInSec +
                "} " + super.toString();
    }
}
