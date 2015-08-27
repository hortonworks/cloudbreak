package com.sequenceiq.cloudbreak.cloud.event.credential;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

public class DeleteCredentialRequest extends CloudPlatformRequest<DeleteCredentialResult> {


    public DeleteCredentialRequest(CloudContext cloudContext, CloudCredential cloudCredential) {
        super(cloudContext, cloudCredential);
    }
}
