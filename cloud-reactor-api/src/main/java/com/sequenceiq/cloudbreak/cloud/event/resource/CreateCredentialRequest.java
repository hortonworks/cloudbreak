package com.sequenceiq.cloudbreak.cloud.event.resource;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

public class CreateCredentialRequest extends CloudPlatformRequest<CreateCredentialResult> {

    public CreateCredentialRequest(CloudContext cloudCtx, CloudCredential cloudCredential) {
        super(cloudCtx, cloudCredential);
    }
}
