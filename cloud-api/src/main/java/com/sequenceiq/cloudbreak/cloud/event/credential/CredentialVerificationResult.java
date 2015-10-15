package com.sequenceiq.cloudbreak.cloud.event.credential;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;

public class CredentialVerificationResult extends CloudPlatformResult {

    private CloudCredentialStatus cloudCredentialStatus;

    public CredentialVerificationResult(CloudPlatformRequest<CredentialVerificationResult> request, CloudCredentialStatus cloudCredentialStatus) {
        super(request);
        this.cloudCredentialStatus = cloudCredentialStatus;
    }

    public CredentialVerificationResult(String statusReason, Exception errorDetails, CloudPlatformRequest<CredentialVerificationResult> request) {
        super(statusReason, errorDetails, request);
    }

    public CloudCredentialStatus getCloudCredentialStatus() {
        return cloudCredentialStatus;
    }
}
