package com.sequenceiq.cloudbreak.cloud.event.setup;

import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.CloudStackRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

import reactor.rx.Promise;

public class SetupRequest<T> extends CloudStackRequest<T> {
    public SetupRequest(CloudContext cloudContext, CloudCredential cloudCredential, CloudStack cloudStack, Promise<T> result) {
        super(cloudContext, cloudCredential, cloudStack, result);
    }
}
