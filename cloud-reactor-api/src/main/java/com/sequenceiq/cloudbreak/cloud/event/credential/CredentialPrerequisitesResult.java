package com.sequenceiq.cloudbreak.cloud.event.credential;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.response.CredentialPrerequisitesResponse;

public class CredentialPrerequisitesResult extends CloudPlatformResult {

    private CredentialPrerequisitesResponse credentialPrerequisitesResponse;

    public CredentialPrerequisitesResult(Long resourceId, CredentialPrerequisitesResponse credentialPrerequisitesResponse) {
        super(resourceId);
        this.credentialPrerequisitesResponse = credentialPrerequisitesResponse;
    }

    public CredentialPrerequisitesResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }

    public CredentialPrerequisitesResponse getCredentialPrerequisitesResponse() {
        return credentialPrerequisitesResponse;
    }
}
