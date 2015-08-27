package com.sequenceiq.cloudbreak.cloud.event.resource;

import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

public class LaunchStackRequest extends CloudStackRequest<LaunchStackResult> {

    public LaunchStackRequest(CloudContext cloudContext, CloudCredential cloudCredential, CloudStack cloudStack) {
        super(cloudContext, cloudCredential, cloudStack);
    }

}
