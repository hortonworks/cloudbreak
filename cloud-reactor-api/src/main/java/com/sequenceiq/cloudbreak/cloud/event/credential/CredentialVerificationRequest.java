package com.sequenceiq.cloudbreak.cloud.event.credential;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

public class CredentialVerificationRequest extends CloudPlatformRequest<CredentialVerificationResult> {

    private final boolean creationVerification;

    public CredentialVerificationRequest(CloudContext cloudContext, CloudCredential cloudCredential) {
        super(cloudContext, cloudCredential);
        this.creationVerification = false;
    }

    public CredentialVerificationRequest(CloudContext cloudContext, CloudCredential cloudCredential, boolean creationVerification) {
        super(cloudContext, cloudCredential);
        this.creationVerification = creationVerification;
    }

    public boolean isCreationVerification() {
        return creationVerification;
    }
}
