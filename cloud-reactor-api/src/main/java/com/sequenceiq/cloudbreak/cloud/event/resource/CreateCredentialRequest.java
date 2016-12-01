package com.sequenceiq.cloudbreak.cloud.event.resource;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

public class CreateCredentialRequest extends CloudStackRequest<CreateCredentialResult> {

    public CreateCredentialRequest(CloudContext cloudCtx, CloudCredential cloudCredential, CloudStack cloudStack) {
        super(cloudCtx, cloudCredential, cloudStack);
    }
}
