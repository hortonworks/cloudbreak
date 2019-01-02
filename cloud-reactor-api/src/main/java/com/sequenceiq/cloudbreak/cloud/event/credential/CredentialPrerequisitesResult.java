package com.sequenceiq.cloudbreak.cloud.event.credential;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses.CredentialPrerequisitesV4Response;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;

public class CredentialPrerequisitesResult extends CloudPlatformResult<CloudPlatformRequest<?>> {

    private CredentialPrerequisitesV4Response credentialPrerequisitesV4Response;

    public CredentialPrerequisitesResult(CloudPlatformRequest<?> request, CredentialPrerequisitesV4Response credentialPrerequisitesV4Response) {
        super(request);
        this.credentialPrerequisitesV4Response = credentialPrerequisitesV4Response;
    }

    public CredentialPrerequisitesResult(String statusReason, Exception errorDetails, CloudPlatformRequest<?> request) {
        super(statusReason, errorDetails, request);
    }

    public CredentialPrerequisitesV4Response getCredentialPrerequisitesV4Response() {
        return credentialPrerequisitesV4Response;
    }
}
