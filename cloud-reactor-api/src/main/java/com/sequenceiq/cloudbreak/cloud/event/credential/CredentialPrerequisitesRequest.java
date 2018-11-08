package com.sequenceiq.cloudbreak.cloud.event.credential;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;

public class CredentialPrerequisitesRequest extends CloudPlatformRequest<CredentialPrerequisitesResult> {

    private final String externalId;

    private final String deploymentAddress;

    public CredentialPrerequisitesRequest(CloudContext cloudContext, String externalId, String deploymentAddress) {
        super(cloudContext, null);
        this.externalId = externalId;
        this.deploymentAddress = deploymentAddress;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getDeploymentAddress() {
        return deploymentAddress;
    }
}
