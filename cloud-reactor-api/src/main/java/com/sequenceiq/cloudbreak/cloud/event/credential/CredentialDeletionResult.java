package com.sequenceiq.cloudbreak.cloud.event.credential;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;

public class CredentialDeletionResult extends CloudPlatformResult {

    private CloudCredentialStatus cloudCredentialStatus;

    public CredentialDeletionResult(Long resourceId, CloudCredentialStatus cloudCredentialStatus) {
        super(resourceId);
        this.cloudCredentialStatus = cloudCredentialStatus;
    }

    public CredentialDeletionResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }

    public CloudCredentialStatus getCloudCredentialStatus() {
        return cloudCredentialStatus;
    }
}
