package com.sequenceiq.cloudbreak.cloud.event.credential;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.common.model.CredentialType;

public class CredentialPrerequisitesRequest extends CloudPlatformRequest<CredentialPrerequisitesResult> {

    private final String externalId;

    private final String deploymentAddress;

    private final CredentialType type;

    public CredentialPrerequisitesRequest(CloudContext cloudContext, String externalId, String deploymentAddress,
        CredentialType type) {
        super(cloudContext, null);
        this.externalId = externalId;
        this.deploymentAddress = deploymentAddress;
        this.type = type;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getDeploymentAddress() {
        return deploymentAddress;
    }

    public CredentialType getType() {
        return type;
    }

}
