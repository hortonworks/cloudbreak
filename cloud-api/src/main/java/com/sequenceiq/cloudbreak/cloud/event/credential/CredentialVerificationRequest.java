package com.sequenceiq.cloudbreak.cloud.event.credential;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

public class CredentialVerificationRequest extends CloudPlatformRequest<CredentialVerificationResult> {

    public CredentialVerificationRequest(CloudContext cloudContext, CloudCredential cloudCredential) {
        super(cloudContext, cloudCredential);
    }
}
