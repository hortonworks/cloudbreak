package com.sequenceiq.cloudbreak.cloud.event.credential;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;

public class DeleteCredentialResult extends CloudPlatformResult {


    private CloudCredentialStatus cloudCredentialStatus;

    public DeleteCredentialResult(CloudPlatformRequest<DeleteCredentialResult> request, CloudCredentialStatus cloudCredentialStatus) {
        super(request);
        this.cloudCredentialStatus = cloudCredentialStatus;
    }

    public DeleteCredentialResult(String statusReason, Exception errorDetails, CloudPlatformRequest<DeleteCredentialResult> request) {
        super(statusReason, errorDetails, request);
    }

    public CloudCredentialStatus getCloudCredentialStatus() {
        return cloudCredentialStatus;
    }
}
