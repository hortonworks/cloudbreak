package com.sequenceiq.cloudbreak.cloud.event.credential;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;

public class CreateCredentialResult extends CloudPlatformResult {

    private CloudCredentialStatus cloudCredentialStatus;

    public CreateCredentialResult(CloudPlatformRequest<CreateCredentialResult> request, CloudCredentialStatus cloudCredentialStatus) {
        super(request);
        this.cloudCredentialStatus = cloudCredentialStatus;
    }

    public CreateCredentialResult(String statusReason, Exception errorDetails, CloudPlatformRequest<CreateCredentialResult> request) {
        super(statusReason, errorDetails, request);
    }

    public CloudCredentialStatus getCloudCredentialStatus() {
        return cloudCredentialStatus;
    }
}
