package com.sequenceiq.cloudbreak.cloud.event.credential;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;

public class CredentialPrerequisitesRequest extends CloudPlatformRequest<CredentialPrerequisitesResult> {

    private String externalId;

    public CredentialPrerequisitesRequest(CloudContext cloudContext, String externalId) {
        super(cloudContext, null);
        this.externalId = externalId;
    }

    public String getExternalId() {
        return externalId;
    }
}
