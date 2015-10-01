package com.sequenceiq.cloudbreak.cloud.event.instance;

import com.sequenceiq.cloudbreak.cloud.event.StackPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

public class GetSSHFingerprintsRequest<T> extends StackPlatformRequest<T> {

    private CloudInstance cloudInstance;

    public GetSSHFingerprintsRequest(CloudContext cloudContext, CloudCredential cloudCredential, CloudInstance cloudInstance, CloudStack cloudStack) {
        super(cloudContext, cloudCredential, cloudStack);
        this.cloudInstance = cloudInstance;
    }

    public CloudInstance getCloudInstance() {
        return cloudInstance;
    }
}
