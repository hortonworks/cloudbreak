package com.sequenceiq.cloudbreak.cloud.event.credential;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;

public class CredentialVerificationResult extends CloudPlatformResult {

    private CloudCredentialStatus cloudCredentialStatus;

    public CredentialVerificationResult(Long resourceId, CloudCredentialStatus cloudCredentialStatus) {
        super(resourceId);
        this.cloudCredentialStatus = cloudCredentialStatus;
    }

    public CredentialVerificationResult(String statusReason, Exception errorDetails, Long resourceId, CloudCredentialStatus cloudCredentialStatus) {
        super(statusReason, errorDetails, resourceId);
        this.cloudCredentialStatus = cloudCredentialStatus;
    }

    public CredentialVerificationResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }

    public CloudCredentialStatus getCloudCredentialStatus() {
        return cloudCredentialStatus;
    }
}
