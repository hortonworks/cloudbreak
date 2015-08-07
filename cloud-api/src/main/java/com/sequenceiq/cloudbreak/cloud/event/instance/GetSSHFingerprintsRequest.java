package com.sequenceiq.cloudbreak.cloud.event.instance;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;

import reactor.rx.Promise;

public class GetSSHFingerprintsRequest<T> extends CloudPlatformRequest<T> {

    private CloudInstance cloudInstance;

    public GetSSHFingerprintsRequest(CloudContext cloudContext, CloudCredential cloudCredential, Promise<T> result, CloudInstance cloudInstance) {
        super(cloudContext, cloudCredential, result);
        this.cloudInstance = cloudInstance;
    }

    public CloudInstance getCloudInstance() {
        return cloudInstance;
    }
}
