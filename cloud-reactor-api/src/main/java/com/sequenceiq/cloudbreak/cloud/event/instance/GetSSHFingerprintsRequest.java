package com.sequenceiq.cloudbreak.cloud.event.instance;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;

public class GetSSHFingerprintsRequest<T> extends CloudPlatformRequest<T> {

    private CloudInstance cloudInstance;

    public GetSSHFingerprintsRequest(CloudContext cloudContext, CloudCredential cloudCredential, CloudInstance cloudInstance) {
        super(cloudContext, cloudCredential);
        this.cloudInstance = cloudInstance;
    }

    public CloudInstance getCloudInstance() {
        return cloudInstance;
    }
}
