package com.sequenceiq.cloudbreak.cloud.event;

import com.sequenceiq.cloudbreak.cloud.event.context.StackContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

import reactor.rx.Promise;

public class PreProvisionCheckRequest<T> extends BaseRequest<T> {
    public PreProvisionCheckRequest(StackContext stackContext, CloudCredential cloudCredential, CloudStack cloudStack, Promise<T> result) {
        super(stackContext, cloudCredential, cloudStack, result);
    }

}
