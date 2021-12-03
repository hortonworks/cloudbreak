package com.sequenceiq.cloudbreak.cloud.event.resource.migration.aws;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.CloudStackRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

public class DeleteCloudFormationRequest extends CloudStackRequest<DeleteCloudFormationResult> {

    public DeleteCloudFormationRequest(CloudContext cloudContext, CloudCredential cloudCredential, CloudStack cloudStack) {
        super(cloudContext, cloudCredential, cloudStack);
    }
}
