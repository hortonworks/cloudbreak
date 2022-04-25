package com.sequenceiq.cloudbreak.cloud.event.instance;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

public class DelayedStopInstancesRequest extends StopInstancesRequest {

    private final long delayInSec;

    public DelayedStopInstancesRequest(CloudContext cloudContext, CloudCredential cloudCredential, List<CloudResource> resources,
            List<CloudInstance> cloudInstances, long delayInSec) {
        super(cloudContext, cloudCredential, resources, cloudInstances);
        this.delayInSec = delayInSec;
    }

    public long getDelayInSec() {
        return delayInSec;
    }

    @Override
    public String toString() {
        return "DelayedStopInstancesRequest{" +
                "delayInSec=" + delayInSec +
                "} " + super.toString();
    }
}
