package com.sequenceiq.cloudbreak.cloud.event.credential;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.response.CredentialPrerequisitesResponse;

public class CredentialPrerequisitesResult extends CloudPlatformResult<CloudPlatformRequest<?>> {

    private CredentialPrerequisitesResponse credentialPrerequisitesResponse;

    public CredentialPrerequisitesResult(CloudPlatformRequest<?> request, CredentialPrerequisitesResponse credentialPrerequisitesResponse) {
        super(request);
        this.credentialPrerequisitesResponse = credentialPrerequisitesResponse;
    }

    public CredentialPrerequisitesResult(String statusReason, Exception errorDetails, CloudPlatformRequest<?> request) {
        super(statusReason, errorDetails, request);
    }

    public CredentialPrerequisitesResponse getCredentialPrerequisitesResponse() {
        return credentialPrerequisitesResponse;
    }
}
