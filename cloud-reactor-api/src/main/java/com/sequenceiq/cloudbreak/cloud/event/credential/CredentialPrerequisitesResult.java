package com.sequenceiq.cloudbreak.cloud.event.credential;

import com.sequenceiq.cloudbreak.api.model.v3.credential.CredentialPrerequisites;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;

public class CredentialPrerequisitesResult extends CloudPlatformResult<CloudPlatformRequest<?>> {

    private CredentialPrerequisites credentialPrerequisites;

    public CredentialPrerequisitesResult(CloudPlatformRequest<?> request, CredentialPrerequisites credentialPrerequisites) {
        super(request);
        this.credentialPrerequisites = credentialPrerequisites;
    }

    public CredentialPrerequisitesResult(String statusReason, Exception errorDetails, CloudPlatformRequest<?> request) {
        super(statusReason, errorDetails, request);
    }

    public CredentialPrerequisites getCredentialPrerequisites() {
        return credentialPrerequisites;
    }
}
