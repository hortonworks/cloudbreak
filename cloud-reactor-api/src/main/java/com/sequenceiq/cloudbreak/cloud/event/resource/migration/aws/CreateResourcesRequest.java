package com.sequenceiq.cloudbreak.cloud.event.resource.migration.aws;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.CloudStackRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

public class CreateResourcesRequest extends CloudStackRequest<CreateResourcesResult> {

    private String hostGroupName;

    public CreateResourcesRequest(CloudContext cloudContext, CloudCredential cloudCredential, CloudStack cloudStack, String hostGroupName) {
        super(cloudContext, cloudCredential, cloudStack);
        this.hostGroupName = hostGroupName;
    }

    public String getHostGroupName() {
        return hostGroupName;
    }
}
