package com.sequenceiq.cloudbreak.cloud.event.resource;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

public class UpscaleStackValidationRequest<T> extends CloudStackRequest<T> {
    public UpscaleStackValidationRequest(CloudContext cloudContext, CloudCredential cloudCredential, CloudStack stack) {
        super(cloudContext, cloudCredential, stack);
    }
}
